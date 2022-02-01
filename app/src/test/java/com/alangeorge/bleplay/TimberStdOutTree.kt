package com.alangeorge.bleplay

import timber.log.Timber

class TimberStdOutTree: Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        println("priority = [${priority}], tag = [${tag}], message = [${message}], throwable = [${t}]")
    }
}