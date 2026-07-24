package com.example.readproplus.pdf

class PdfPasswordCache {
    private val passwords = mutableMapOf<String, String>()

    @Synchronized
    fun put(uri: String, password: String) {
        passwords[uri] = password
    }

    @Synchronized
    fun get(uri: String): String? = passwords[uri]

    @Synchronized
    fun remove(uri: String) {
        passwords.remove(uri)
    }

    @Synchronized
    fun clear() = passwords.clear()
}
