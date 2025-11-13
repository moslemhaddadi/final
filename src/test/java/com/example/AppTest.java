package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigourous Test :-)
     */
    @Test
    public void testApp()
    {
        assertTrue( true );
    }

    /**
     * Test de la méthode add()
     */
    @Test
    public void testAdd()
    {
        assertEquals( 8, App.add(5, 3) );
        assertEquals( -2, App.add(-5, 3) );
        assertEquals( 0, App.add(0, 0) );
    }
}
