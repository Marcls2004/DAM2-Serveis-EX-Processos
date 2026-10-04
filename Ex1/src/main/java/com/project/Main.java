package com.project;

// Importació de CompletableFuture per gestionar tasques asíncrones en cadena
import java.util.concurrent.CompletableFuture;

public class Main {

    public static void main(String[] args) {
        
        System.out.println("[Fil Principal] Iniciant el procés de la sol·licitud web de forma asíncrona...\n");

        // ENCADENAMENT ASÍNCRON AMB COMPLETABLEFUTURE
        // Iniciem una cadena on cada baula s'executarà quan finalitzi l'anterior sense bloquejar el fil principal.
        CompletableFuture<Void> cadenaAsincrona = CompletableFuture
                
                // =======================================================================
                // ETAPA 1 (supplyAsync): Validació de les dades d'entrada
                // Executa una tasca asíncrona que RETORNA un valor inicial (un String).
                // =======================================================================
                .supplyAsync(() -> {
                    try {
                        System.out.println("[" + Thread.currentThread().getName() + "] Etapa 1: Validant les dades de la sol·licitud...");
                        Thread.sleep(1200); // Simula el temps que triga a comprovar les dades a la base de dades
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("[" + Thread.currentThread().getName() + "] Etapa 1: Dades validades correctament.");
                    return "usuari_web_pro"; // Retorna el valor inicial que passarà a la següent etapa
                })

                // =======================================================================
                // ETAPA 2 (thenApply): Processament i càlcul de les dades
                // Rep el resultat de l'etapa anterior, el MODIFICA i en retorna un de nou.
                // =======================================================================
                .thenApply((nomUsuari) -> {
                    try {
                        System.out.println("[" + Thread.currentThread().getName() + "] Etapa 2: Processant dades i calculant resultats per a: " + nomUsuari);
                        Thread.sleep(1000); // Simula un càlcul complex de lògica de negoci
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    String resultatCalculat = "Token_Generat_Per_" + nomUsuari.toUpperCase();
                    System.out.println("[" + Thread.currentThread().getName() + "] Etapa 2: Càlcul finalitzat.");
                    return resultatCalculat; // Retorna el text modificat a la següent etapa
                })

                // =======================================================================
                // ETAPA 3 (thenAccept): Mostrar la resposta final al client
                // Rep el resultat de l'etapa anterior (Etapa 2), el CONSUMEIX i no retorna res (Void).
                // =======================================================================
                .thenAccept((resultatFinal) -> {
                    try {
                        System.out.println("[" + Thread.currentThread().getName() + "] Etapa 3: Preparant la resposta HTTP final...");
                        Thread.sleep(800); // Simula la renderització o enviament de la pàgina web
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // Simulem la presentació final de la resposta enviada a l'usuari
                    System.out.println("\n--- RESPOSTA ENVIADA A L'USUARI ---");
                    System.out.println("Sol·licitud processada correctament. Resposta del sistema: " + resultatFinal);
                    System.out.println("-----------------------------------");
                });

        // FIL PRINCIPAL DISPONIBLE
        // Com que la cadena és totalment asíncrona i no bloquejant, el fil 'main' pot continuar 
        // executant altres instruccions immediatament mentre les etapes es fan en segon pla.
        System.out.println("[Fil Principal] La cadena asíncrona s'està executant en segon pla. Jo no estic bloquejat!");
        System.out.println("[Fil Principal] Fent altres tasques del sistema...\n");

        // ESPERA FINAL (join)
        // Requisit: Usem join() per forçar el fil principal a esperar que TOTA la cadena asíncrona 
        // es completi. Si no poséssim aquesta línia, el programa finalitzaria abans que les tasques 
        // en segon pla acabessin de mostrar els resultats per pantalla.
        cadenaAsincrona.join();

        System.out.println("\n[Fil Principal] Totes les operacions asíncrones han acabat. Tancant l'aplicació web.");
    }
}
