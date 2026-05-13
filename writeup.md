# Motivation
*The small farmer needs answers fast. The provider needs to reach further. The knowledge exists, the products exist, the technology exists. What's been missing is the bridge. That's Agrogem.*

Our team has been working in Guatemala's agroindustrial sector, building Q&A AI agents for agroindustrias across WhatsApp and Meta Messenger. As we analyzed the data, a pattern emerged: farmers struggling to diagnose problems in their own crops. "I have some spiders on my plants" or "I'm not an expert, but my neighbor's been using this product." Getting to the right solution always starts with the right diagnosis, and the faster it happens, the less a farmer loses.

And the stakes are real. Agriculture is how one in three Guatemalans make a living, [70% of the country's poorest people are out in rural areas](https://www.ifad.org/en/w/countries/guatemala) where farming is all they have. [Nearly 80% of Guatemala's indigenous population lives in poverty](https://www.wfp.org/countries/guatemala). The country has the [highest childhood stunting rate in Latin America, sixth in the world](https://www.brookings.edu/articles/rural-poverty-climate-change-and-family-migration-from-guatemala/), and it hits hardest in rural communities.

## Operative improvements
We went beyond the data. We spoke directly with a team from Agroindustrias Suceso, a leading company in biological products for pest control and crop productivity, interviewing their CEO and farmers working in the field. What we found was a broken, slow-moving chain: a farmer notices a problem, contacts a local agrocenter for help, waits 2 to 4 days for someone to come assess it, then once a diagnosis is finally made, reaches out to a supplier and waits another 4 to 6 days for the product to arrive. That is nearly 10 days of crop damage, stress, and lost yield before any solution is applied.

## Modernization improvements
Agriculture in Guatemala remains largely traditional. Small farmers often take on debt to finance each sowing season, making it a high-stakes investment with little room for error. When something goes wrong, the consequences can be severe for families who depend on their harvest to survive. This pressure pushes farmers toward familiar habits: reusing the same pesticides season after season, applying incorrect doses, and avoiding new approaches out of fear that a mistake will cost them everything. That resistance to change carries its own risks. Repeated misuse of chemical products at wrong doses can lead to pest resistance, giving rise to harder-to-control infestations over time. Meanwhile, more effective and ecological alternatives, like biological pest control products, go unused simply because farmers lack the guidance to trust them.

Agrogem can bridge that gap. Beyond fast diagnosis, it can serve as a trusted advisor, teaching farmers how to apply products at the correct doses, explaining the advantages of biological over chemical solutions, and giving them the confidence to make better decisions without fear.

# Solution Approach

Agrogem could help to reduce and simplify diagnosis and solution time. We are approaching this in two phases. Phase one, the focus of this project, is building a model that diagnoses pests and detects crop health problems from images and descriptions. Phase two will be product recommendations based on the diagnosis. 

The biggest advantage of Gemma 4 is that it runs entirely on-device, opening the door to apps that weren't possible before in terms of computing cost, accessibility, and intelligence. We harnessed a simple supervisor-agent loop to do the job.  Agrogem has online and offline mode. 

## Online mode 
Gemma 4 uses function calling to use a set of REST API of agronomic tools (weather, soil conditions, pest and disease risk, irrigation, and harvest windows) so the model can reason over live field data, not approximations. A farmer can ask "is there frost risk on my farm this week?" or "when is the best dosis to harvest my coffee?" and get a concrete answer in Spanish.

## Offline mode 
Gemma4 runs purely on local inference. For farmers in rural Guatemala, offline it's the default. By processing images locally, Agrogem delivers instant plant disease diagnosis, affected zone detection, and suggests an action plan without requiring internet. We can also fine-tune the model with regional crop data to improve detection accuracy even further.

## Mobil app
We chose Kotlin Multiplatform for Android and future iOS compatibility, designed to run on mid-range devices the hardware most farmers actually carry but also capable on the more powerful phones used by agricultural engineers. 

# Challenges we faced:

## Building the training dataset.
One of the first walls we hit was data. Labeled images of crop diseases specific to Guatemala's agricultural context simply don't exist at the scale a fine-tuned model needs. We know this is something we'll have to keep improving the training base we have today is a starting point.

To move forward, we generated synthetic training data using the gemini-3-flash-preview,  to produce examples in the exact format Gemma 4 requires for fine-tuning. It's not a perfect solution, but it let us build something real while we work on collecting ground-truth 
data from the field.

## Training and exporting the model.

Our local machines (16 GB of RAM) couldn't handle the workload. We used Kaggle's free 30-hour GPU quota to run the fine-tuning with Unsloth, which was enough to get the job done but left no room for experimentation. Every run had to count.

Once trained, we uploaded the model to HuggingFace. Getting it onto Android required converting it to the .litertlm format that LiteRT accepts  and that conversion wasn't straightforward. The official tooling doesn't yet support multimodal Gemma 4 out of the box, so our fine-tuned model currently runs text-only. The final compilation also exceeded what any of our machines could handle, so we moved that step to Google Cloud.

The production app uses the official Gemma 4 bundle for full image support; once upstream tooling catches up, we switch to our fine-tuned model.

## Distributing the model
Including a multi-gigabyte file in the APK wasn't an option. But downloading a 2.5 GB file over a rural connection introduces its own problem: a download that fails halfway and forces the user to start over is a fast way to lose them before they've even tried the app. We distribute the model separately from the app. The APK installs a lightweight base, and when the user needs local Gemma capabilities, the device downloads the .litertlm file from HuggingFace, (versioned hosting without requiring us to manage bandwidth or infrastructure).

The download runs as a coroutine on Dispatchers.IO, gated by network constraints. If the user closes the app, partial bytes are preserved and the next launch resumes via HTTP Range requests. The file downloads to a temporary .tmp path and is only renamed to .litertlm once the transfer completes cleanly, preventing a broken initialization. On failure, the downloader retries up to 6 times with exponential backoff (1.5 s → 30 s). If the model is ultimately unavailable or a request exceeds its capabilities, the app falls back to the remote backend so the farmer always gets an answer.

## Local Storage and Sync
Farmers in rural Guatemala often work without a reliable connection, so we made offline the default, not a fallback. Every conversation, diagnosis, and recommendation is stored on the device using SQLite via SQLDelight. When signal is available, the app reaches the backend to enrich responses with live weather, soil, and regional risk data. But the experience never waits for it.

## Language as a Feature 
Agrogem responds entirely in Spanish. It's a deliberate design decision. The farmers we built this for don't speak English. Every diagnosis, action plan, and recommendation is delivered in their language, without technical jargon, in a tone that respects that they already know their land. 

## Tech Stack 
Agrogem runs on Kotlin Multiplatform with on-device inference via LiteRT and a FastAPI backend exposing 12 agronomic endpoints as function-calling tools. Fine-tuning is handled by Unsloth, persistence by SQLDelight. The backend uses MongoDB, Redis, and pulls from Open-Meteo, NASA POWER, and ISRIC SoilGrids so the model reasons over real field data.

# What's next
On the technical side, the immediate priorities are hardening Agrogem itself: HTTP Range Requests for resumable downloads, tighter error handling, and replacing the synthetic dataset with real ground-truth data from Guatemalan fields. In parallel, we're exploring Google's multimodal embedding API to build a regional pest and disease library, either as a RAG layer on top of the current model or as the foundation for the next fine-tuning run.

The more strategic work is integration. We already run a Gemini-powered agent on WhatsApp and Messenger, and Agrogem was always meant to connect with it. The vision is a complete loop: the local model diagnoses the problem in the field, and when signal returns, the cloud agent recommends a specific biological product and helps close the order. That's the version that delivers real impact.

# Acknowledgments
We extend our sincere gratitude to Eduardo Solares, Crista Rosenberg, and the entire team at Agroindustrias Suceso for opening the doors of the agricultural sector to us. Their trust from Balam's earliest days and their continued partnership as we move toward a field pilot together gave us the access, knowledge, and grounding that made this project possible.
