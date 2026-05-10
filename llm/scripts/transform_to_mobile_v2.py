#!/usr/bin/env python3
"""
transform_to_mobile_v2.py — Workaround para Gemma 4 vision fine-tuneado → .litertlm

Por qué existe este script:
  litert-torch tiene 4 bugs abiertos que bloquean el export end-to-end de Gemma 4
  vision fine-tuneado (issues #997, #998, #1001, #1005 en google-ai-edge/litert-torch).
  El script v1 (transform_to_mobile.py) crashea en "Package model" con
  TypeError: 'NoneType' object is not subscriptable en image_processor.size['height'].

Este v2 NO usa el path roto. En su lugar:

  1) Descarga el bundle pre-built oficial (litert-community/gemma-4-E2B-it-litert-lm)
  2) Lo "peekea" → extrae metadata válida + vision_encoder + tokenizer + model.toml
  3) Trasplanta SOLO nuestro decoder TFLite fine-tuneado en la estructura oficial
  4) Re-empaqueta con litert-lm-builder (paquete pip independiente del roto litert-torch)

Limitaciones (leer antes de usar):
  - El LoRA de visión SE PIERDE. El export público del vision encoder de Gemma 4
    está roto (Unsupported model type: gemma4). Usamos el vision encoder oficial.
  - SIGSEGV en runtime es probable: el decoder de litert-torch puede tener firmas
    incompatibles (fp32 vs int8 KV cache, falta param_tensor, falta signature verify)
    que el Gemma4DataProcessor del runtime rechaza. Mismo síntoma que QuadraKev (#998).
  - Es un workaround de deadline, no un path de producción.

Uso típico:
  pip install -U huggingface_hub litert-lm-builder

  python transform_to_mobile_v2.py \\
      --finetuned-decoder /workspace/exports/model.tflite \\
      --output ./agrogem.litertlm \\
      --hf-token "$HF_TOKEN"

  # Opcional: subir a HF
  python transform_to_mobile_v2.py ... --upload-repo alvarog1318/agrogem-litertlm

Refs:
  - https://github.com/google-ai-edge/LiteRT-LM/blob/main/python/litert_lm_builder/litertlm_builder.py
  - https://github.com/google-ai-edge/litert-torch/issues/998
  - https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

try:
    import tomllib  # Python 3.11+
except ImportError:
    sys.exit("ERROR: requiere Python 3.11+ (tomllib).")

try:
    import tomli_w
except ImportError:
    sys.exit("ERROR: pip install tomli_w")

try:
    from huggingface_hub import HfApi, create_repo, hf_hub_download, login
except ImportError:
    sys.exit("ERROR: pip install -U huggingface_hub")


# Defaults verificados contra el repo oficial al 2026-05-09.
OFFICIAL_REPO = "litert-community/gemma-4-E2B-it-litert-lm"
OFFICIAL_FILENAME = "gemma-4-E2B-it.litertlm"  # CPU/GPU build (~2.59 GB)
DEFAULT_MAX_NUM_TOKENS = 4096  # Coincide con la nota de memory-tuning del proyecto.


# ---------- helpers ----------

def log(msg: str) -> None:
    print(f"[v2] {msg}", flush=True)


def run(cmd: list[str], **kwargs) -> subprocess.CompletedProcess:
    log(f"$ {' '.join(cmd)}")
    return subprocess.run(cmd, check=True, **kwargs)


def ensure_cli_exists(name: str) -> None:
    if shutil.which(name) is None:
        sys.exit(
            f"ERROR: el CLI '{name}' no está en PATH. "
            f"Instala con: pip install -U litert-lm-builder"
        )


# ---------- pasos ----------

def download_official_bundle(work_dir: Path, hf_token: str | None) -> Path:
    """Baja el .litertlm oficial pre-built desde litert-community."""
    log(f"Descargando {OFFICIAL_REPO}/{OFFICIAL_FILENAME} ...")
    path = hf_hub_download(
        repo_id=OFFICIAL_REPO,
        filename=OFFICIAL_FILENAME,
        token=hf_token,
        local_dir=str(work_dir / "official"),
    )
    log(f"  → {path} ({Path(path).stat().st_size / 1e9:.2f} GB)")
    return Path(path)


def peek_bundle(official_litertlm: Path, peeked_dir: Path) -> Path:
    """
    Ejecuta `litert-lm-peek --dump_files_dir=...` para extraer todas las secciones
    (decoder, embedder, vision_encoder, vision_adapter, tokenizer, metadata) +
    un model.toml replay-ready.
    """
    ensure_cli_exists("litert-lm-peek")
    if peeked_dir.exists():
        shutil.rmtree(peeked_dir)
    peeked_dir.mkdir(parents=True)

    run([
        "litert-lm-peek",
        "--litertlm_file", str(official_litertlm),
        "--dump_files_dir", str(peeked_dir),
    ])

    toml_path = peeked_dir / "model.toml"
    if not toml_path.exists():
        sys.exit(
            f"ERROR: peek no generó model.toml en {peeked_dir}. "
            f"Probablemente la versión de litert-lm-builder es vieja. "
            f"Actualiza con: pip install -U litert-lm-builder"
        )
    log(f"Peek OK. Sections en {peeked_dir}:")
    for f in sorted(peeked_dir.iterdir()):
        log(f"  - {f.name} ({f.stat().st_size / 1e6:.1f} MB)")
    return toml_path


def find_section_idx(sections: list[dict], model_type: str) -> int | None:
    """Busca el índice del [[sections]] cuyo model_type coincide."""
    for i, s in enumerate(sections):
        if s.get("model_type") == model_type:
            return i
    return None


def patch_toml(
    peeked_toml: Path,
    finetuned_decoder: Path,
    finetuned_embedder: Path | None,
    finetuned_per_layer_embedder: Path | None,
    max_num_tokens: int,
    output_toml: Path,
) -> None:
    """
    Lee el model.toml extraído, reemplaza el data_path del PREFILL_DECODE
    (y opcionalmente del EMBEDDER / PER_LAYER_EMBEDDER) con nuestros TFLites
    fine-tuneados. Deja vision_encoder + vision_adapter + tokenizer + metadata
    apuntando a los archivos oficiales peekeados.
    """
    log(f"Leyendo TOML peekeado: {peeked_toml}")
    with open(peeked_toml, "rb") as f:
        cfg = tomllib.load(f)

    sections = cfg.get("sections", [])
    if not sections:
        sys.exit(f"ERROR: el TOML peekeado no tiene [[sections]]. ¿Versión incorrecta?")

    log(f"Sections originales ({len(sections)}):")
    for s in sections:
        mt = s.get("model_type", s.get("section_type", "?"))
        dp = s.get("data_path", "?")
        log(f"  - {mt}: {dp}")

    # Trasplante del decoder fine-tuneado.
    idx = find_section_idx(sections, "PREFILL_DECODE")
    if idx is None:
        sys.exit("ERROR: no se encontró [[sections]] con model_type=PREFILL_DECODE.")
    log(f"Reemplazando PREFILL_DECODE.data_path → {finetuned_decoder}")
    sections[idx]["data_path"] = str(finetuned_decoder.resolve())

    # Embedder externo (si lo exportamos con --externalize_embedder).
    if finetuned_embedder is not None:
        idx = find_section_idx(sections, "EMBEDDER")
        if idx is not None:
            log(f"Reemplazando EMBEDDER.data_path → {finetuned_embedder}")
            sections[idx]["data_path"] = str(finetuned_embedder.resolve())
        else:
            log("WARN: --finetuned-embedder dado pero no hay sección EMBEDDER en el oficial.")

    if finetuned_per_layer_embedder is not None:
        idx = find_section_idx(sections, "PER_LAYER_EMBEDDER")
        if idx is not None:
            log(f"Reemplazando PER_LAYER_EMBEDDER.data_path → {finetuned_per_layer_embedder}")
            sections[idx]["data_path"] = str(finetuned_per_layer_embedder.resolve())
        else:
            log("WARN: --finetuned-per-layer-embedder dado pero no hay sección PER_LAYER_EMBEDDER.")

    # Tweak del max_num_tokens en el LlmMetadataProto.pbtext si corresponde.
    # (El .pbtext se reempaqueta tal cual; sólo lo editamos como texto plano para no
    # depender de generar el descriptor de proto en este script.)
    pbtext_path = peeked_toml.parent / "LlmMetadataProto.pbtext"
    if pbtext_path.exists() and max_num_tokens != DEFAULT_MAX_NUM_TOKENS:
        log(f"Editando LlmMetadataProto.pbtext: max_num_tokens → {max_num_tokens}")
        text = pbtext_path.read_text()
        import re
        new_text, n = re.subn(
            r"^(\s*max_num_tokens\s*:\s*)\d+",
            rf"\g<1>{max_num_tokens}",
            text,
            count=1,
            flags=re.MULTILINE,
        )
        if n == 0:
            log("WARN: no se encontró max_num_tokens en el .pbtext; se deja igual.")
        else:
            pbtext_path.write_text(new_text)

    cfg["sections"] = sections
    log(f"Escribiendo TOML modificado: {output_toml}")
    with open(output_toml, "wb") as f:
        tomli_w.dump(cfg, f)


def build_bundle(toml_path: Path, output_litertlm: Path) -> None:
    """
    Empaqueta el .litertlm usando el CLI estable de litert-lm-builder.
    Equivalente Python: LitertLmFileBuilder.from_toml_file(toml).build(stream).
    """
    ensure_cli_exists("litert-lm-builder")
    output_litertlm.parent.mkdir(parents=True, exist_ok=True)
    if output_litertlm.exists():
        output_litertlm.unlink()
    run([
        "litert-lm-builder",
        "toml", "--path", str(toml_path),
        "output", "--path", str(output_litertlm),
    ])
    log(f"Bundle generado: {output_litertlm} ({output_litertlm.stat().st_size / 1e9:.2f} GB)")


def verify_bundle(output_litertlm: Path) -> None:
    """Corre peek sobre el bundle nuevo para confirmar que las secciones quedaron OK."""
    ensure_cli_exists("litert-lm-peek")
    log("Verificando bundle generado (peek):")
    run([
        "litert-lm-peek",
        "--litertlm_file", str(output_litertlm),
    ])


def upload_to_hf(output_litertlm: Path, repo: str, hf_token: str) -> None:
    log(f"Subiendo a Hugging Face: {repo}")
    create_repo(repo_id=repo, repo_type="model", exist_ok=True, token=hf_token)
    HfApi().upload_file(
        path_or_fileobj=str(output_litertlm),
        path_in_repo=output_litertlm.name,
        repo_id=repo,
        repo_type="model",
        token=hf_token,
        commit_message="AgroGem v2: fine-tuned decoder transplant into official Gemma 4 bundle",
    )
    log(f"  → https://huggingface.co/{repo}")


# ---------- main ----------

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Trasplante de decoder fine-tuneado en bundle oficial de Gemma 4 (.litertlm).",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    p.add_argument(
        "--finetuned-decoder", required=True, type=Path,
        help="Ruta al model.tflite fine-tuneado (lo dejó tu corrida de export_hf en work_dir).",
    )
    p.add_argument(
        "--finetuned-embedder", type=Path, default=None,
        help="(Opcional) embedder.tflite si exportaste con --externalize_embedder.",
    )
    p.add_argument(
        "--finetuned-per-layer-embedder", type=Path, default=None,
        help="(Opcional) per_layer_embedder.tflite si exportaste con --single_token_embedder.",
    )
    p.add_argument(
        "--output", required=True, type=Path,
        help="Ruta de salida del .litertlm fusionado.",
    )
    p.add_argument(
        "--work-dir", type=Path, default=Path("./_v2_workdir"),
        help="Directorio de trabajo para descargar/peekear (default: ./_v2_workdir).",
    )
    p.add_argument(
        "--hf-token", default=os.environ.get("HF_TOKEN"),
        help="Token de HuggingFace (o env var HF_TOKEN).",
    )
    p.add_argument(
        "--max-num-tokens", type=int, default=DEFAULT_MAX_NUM_TOKENS,
        help=f"Override de max_num_tokens en metadata (default: {DEFAULT_MAX_NUM_TOKENS}).",
    )
    p.add_argument(
        "--upload-repo", default=None,
        help="(Opcional) sube el bundle resultante a este repo de HF (e.g., user/agrogem-litertlm).",
    )
    p.add_argument(
        "--skip-verify", action="store_true",
        help="No correr peek de verificación al final.",
    )
    return p.parse_args()


def main() -> int:
    args = parse_args()

    if not args.finetuned_decoder.exists():
        sys.exit(
            f"ERROR: no existe {args.finetuned_decoder}.\n"
            f"  ¿Tu cloud script borró work_dir? Re-corre export_hf con keep_temporary_files=True\n"
            f"  y pasa la ruta del model.tflite resultante."
        )

    if args.hf_token:
        login(token=args.hf_token, add_to_git_credential=False)

    args.work_dir.mkdir(parents=True, exist_ok=True)

    # 1. Bajar oficial.
    official = download_official_bundle(args.work_dir, args.hf_token)

    # 2. Peek → extraer todo.
    peeked_dir = args.work_dir / "peeked"
    peeked_toml = peek_bundle(official, peeked_dir)

    # 3. Trasplante en TOML.
    patched_toml = args.work_dir / "patched.toml"
    patch_toml(
        peeked_toml=peeked_toml,
        finetuned_decoder=args.finetuned_decoder,
        finetuned_embedder=args.finetuned_embedder,
        finetuned_per_layer_embedder=args.finetuned_per_layer_embedder,
        max_num_tokens=args.max_num_tokens,
        output_toml=patched_toml,
    )

    # 4. Build.
    build_bundle(patched_toml, args.output)

    # 5. Verify.
    if not args.skip_verify:
        verify_bundle(args.output)

    # 6. (Opcional) subir.
    if args.upload_repo:
        if not args.hf_token:
            sys.exit("ERROR: --upload-repo requiere --hf-token.")
        upload_to_hf(args.output, args.upload_repo, args.hf_token)

    log("✅ Done. Próximo paso: probar el bundle on-device.")
    log("   Si crashea en RunPrefillAsync, es el bug conocido de signatures de litert-torch")
    log("   (issue #998) — no es problema del script. Caer al Plan B.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
