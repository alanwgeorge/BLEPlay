package com.alangeorge.bleplay.model

import arrow.core.Either

data class SnackbarMessage(val message: Either<Int, String>)