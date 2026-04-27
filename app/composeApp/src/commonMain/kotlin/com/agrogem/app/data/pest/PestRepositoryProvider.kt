package com.agrogem.app.data.pest

import com.agrogem.app.data.pest.domain.PestRepository

/**
 * Platform-specific creation of [PestRepository].
 */
expect fun createPestRepository(): PestRepository
