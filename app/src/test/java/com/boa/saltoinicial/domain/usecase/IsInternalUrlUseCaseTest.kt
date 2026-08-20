package com.boa.saltoinicial.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsInternalUrlUseCaseTest {

    private val isInternalUrl = IsInternalUrlUseCase("https://www.saltoinicial.com.ar/")

    @Test
    fun `the site itself is internal`() {
        assertTrue(isInternalUrl("https://www.saltoinicial.com.ar/"))
        assertTrue(isInternalUrl("https://saltoinicial.com.ar/2024/03/nota.html"))
        assertTrue(isInternalUrl("http://www.saltoinicial.com.ar/search?q=x"))
    }

    @Test
    fun `subdomains of the site are internal`() {
        assertTrue(isInternalUrl("https://blog.saltoinicial.com.ar/x"))
    }

    @Test
    fun `the host comparison is case insensitive`() {
        assertTrue(isInternalUrl("HTTPS://WWW.SALTOINICIAL.COM.AR/nota"))
    }

    @Test
    fun `other domains are external`() {
        assertFalse(isInternalUrl("https://www.google.com/"))
        assertFalse(isInternalUrl("https://twitter.com/intent/tweet"))
    }

    @Test
    fun `a domain that merely ends with the site name is external`() {
        // "notsaltoinicial.com.ar" no debe pasar por el chequeo de subdominio
        assertFalse(isInternalUrl("https://notsaltoinicial.com.ar/"))
        assertFalse(isInternalUrl("https://saltoinicial.com.ar.evil.com/"))
    }

    @Test
    fun `non http schemes are external`() {
        assertFalse(isInternalUrl("mailto:hola@saltoinicial.com.ar"))
        assertFalse(isInternalUrl("tel:+5493400000000"))
        assertFalse(isInternalUrl("whatsapp://send?text=hola"))
        assertFalse(isInternalUrl("intent://scan/#Intent;scheme=zxing;end"))
    }

    @Test
    fun `malformed or empty urls are external`() {
        assertFalse(isInternalUrl(""))
        assertFalse(isInternalUrl("no es una url"))
        assertFalse(isInternalUrl("javascript:alert(1)"))
    }
}
