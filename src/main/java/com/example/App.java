package com.example;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        System.out.println( "Le résultat de l'addition est : " + add(5, 3) );
    }

    /**
     * Méthode simple pour l'addition.
     * @param a Premier nombre
     * @param b Deuxième nombre
     * @return La somme de a et b
     */
    public static int add(int a, int b) {
        return a + b;
    }
}
