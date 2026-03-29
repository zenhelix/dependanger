package io.github.zenhelix.dependanger.cli

import kotlinx.coroutines.runBlocking

public object CoroutineRunner {
    public fun <T> run(block: suspend () -> T): T = runBlocking { block() }
}
