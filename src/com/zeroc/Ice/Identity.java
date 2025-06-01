package com.zeroc.Ice;

/**
 * Clase que representa la identidad de un objeto ICE.
 * Esta es una implementación mínima para permitir la compilación del proyecto.
 */
public class Identity {
    
    /**
     * Nombre de la identidad
     */
    public String name;
    
    /**
     * Categoría de la identidad
     */
    public String category;
    
    /**
     * Constructor por defecto
     */
    public Identity() {
        this.name = "";
        this.category = "";
    }
    
    /**
     * Constructor con nombre
     * 
     * @param name Nombre de la identidad
     */
    public Identity(String name) {
        this.name = name;
        this.category = "";
    }
    
    /**
     * Constructor con nombre y categoría
     * 
     * @param name Nombre de la identidad
     * @param category Categoría de la identidad
     */
    public Identity(String name, String category) {
        this.name = name;
        this.category = category;
    }
} 