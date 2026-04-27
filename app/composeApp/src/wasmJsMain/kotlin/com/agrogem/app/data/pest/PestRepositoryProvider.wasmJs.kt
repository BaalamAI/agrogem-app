package com.agrogem.app.data.pest

import com.agrogem.app.data.pest.domain.PestRepository
import com.agrogem.app.data.pest.domain.PestResult
import com.agrogem.app.data.pest.domain.PestFailure

actual fun createPestRepository(): PestRepository = NoOpPestRepository()

private class NoOpPestRepository : PestRepository {
    override suspend fun identify(image: com.agrogem.app.data.ImageResult): PestResult {
        return PestResult.Failure(PestFailure.UnsupportedPlatform)
    }
}
