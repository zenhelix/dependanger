package io.github.zenhelix.dependanger.core

public object DependangerPaths {
    public const val CACHE_ROOT: String = ".dependanger/cache"

    public const val BOM_CACHE_DIR: String = "$CACHE_ROOT/bom"
    public const val VERSIONS_CACHE_DIR: String = "$CACHE_ROOT/versions"
    public const val SECURITY_CACHE_DIR: String = "$CACHE_ROOT/security"
    public const val TRANSITIVES_CACHE_DIR: String = "$CACHE_ROOT/transitives"
    public const val LICENSES_CACHE_DIR: String = "$CACHE_ROOT/licenses"

    public fun resolveInUserHome(relativePath: String): String =
        System.getProperty("user.home") + "/" + relativePath
}
