package com.pos.venta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnmascararTest {

    @Test
    void elCorreoMuestraLasPrimerasYUltimasLetrasAntesDeLaArrobaYElDominio() {
        assertEquals("an****es@correo.test", Enmascarar.correo("ana.torres@correo.test"));
        assertEquals("ju****ez@gmail.com", Enmascarar.correo("  juan.perez@gmail.com "));
    }

    @Test
    void unCorreoCortoMuestraSoloLaPrimeraLetra() {
        assertEquals("a****@x.co", Enmascarar.correo("ana@x.co"));
        assertEquals("****@x.co", Enmascarar.correo("@x.co"));
    }

    @Test
    void unCorreoSinArrobaTambienSeEnmascara() {
        assertEquals("al****", Enmascarar.correo("alvaro"));
        assertEquals("****", Enmascarar.correo("ab"));
    }

    @Test
    void elTelefonoMuestraSoloLosPrimerosTresDigitos() {
        assertEquals("315*******", Enmascarar.telefono("3150245878"));
        assertEquals("***", Enmascarar.telefono("123"));
    }

    @Test
    void laDireccionMuestraSoloElComienzo() {
        assertEquals("Calle 10***", Enmascarar.direccion("Calle 10 # 5-20, Cali"));
        assertEquals("***", Enmascarar.direccion("Cra 5"));
    }

    @Test
    void elDocumentoMuestraLosDosPrimerosYLosDosUltimosDigitos() {
        assertEquals("10******10", Enmascarar.documento("1010101010"));
        assertEquals("90******-8", Enmascarar.documento("901234567-8"));
        assertEquals("****", Enmascarar.documento("1234"));
    }

    @Test
    void sinDatoNoHayNadaQueEnmascarar() {
        assertNull(Enmascarar.correo(null));
        assertNull(Enmascarar.correo("  "));
        assertNull(Enmascarar.telefono(null));
        assertNull(Enmascarar.documento(null));
        assertNull(Enmascarar.direccion(" "));
    }
}
