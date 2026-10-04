package com.project;

// Importacions de les utilitats de concurrència de Java
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        
        // ESTRUCTURA DE DADES CONCURRENT COMPARTIDA
        // Creem un mapa segur per a fils. Evita problemes de corrupció de dades 
        // quan múltiples fils llegeixen i escriuen alhora sense bloquejar tot el mapa.
        ConcurrentHashMap<String, Double> compteBancari = new ConcurrentHashMap<>();
        
        // MECANISMES DE SINCRONITZACIÓ (SEMÀFORS / BARRERES)
        // CountDownLatch actua com una barrera amb un comptador inicial d'1.
        // Els fils esperaran fins que el comptador arribi a 0 per poder continuar.
        CountDownLatch latchInicialitzat = new CountDownLatch(1); // Coordina el pas de la Tasca 1 a la 2
        CountDownLatch latchModificat = new CountDownLatch(1);    // Coordina el pas de la Tasca 2 a la 3

        // ==========================================
        // TASCA 1 (Runnable): Recepció de l'operació
        // ==========================================
        Runnable tascaRecepcio = () -> {
            try {
                System.out.println("[Tasca 1] Simulant recepció de l'operació...");
                Thread.sleep(1000); // Simula temps d'espera o latència de xarxa
                
                // Inserció de les dades inicials de forma segura
                compteBancari.put("saldo", 1000.0);
                System.out.println("[Tasca 1] Saldo inicial ingressat: 1000.0 EUR");
            } catch (InterruptedException e) {
                // Si el fil s'interromp, restaurem l'estat d'interrupció
                Thread.currentThread().interrupt();
            } finally {
                // El bloc 'finally' s'executa SEMPRE.
                // Reduïm el comptador a 0 per obrir la barrera i permetre que la Tasca 2 s'activi.
                latchInicialitzat.countDown(); 
            }
        };

        // ==========================================
        // TASCA 2 (Runnable): Càlcul d'interessos
        // ==========================================
        Runnable tascaInteressos = () -> {
            try {
                // El fil es queda congelat aquí fins que latchInicialitzat arribi a 0 (Tasca 1 finalitzada)
                latchInicialitzat.await(); 
                System.out.println("[Tasca 2] Aplicant comissions i càlcul d'interessos...");
                Thread.sleep(1000); // Simula el temps de procés del càlcul
                
                // MODIFICACIÓ ATÒMICA SEGURA
                // computeIfPresent garanteix que la lectura, el càlcul i l'escriptura del nou saldo
                // es facin com una única operació indivisible (atòmica) per evitar conflictes concurrents.
                compteBancari.computeIfPresent("saldo", (clau, saldoActual) -> {
                    double comissio = 5.0;
                    double interessos = saldoActual * 0.02; // Calculem el 2% del saldo
                    return saldoActual - comissio + interessos; // Retorna el saldo modificat
                });
                System.out.println("[Tasca 2] Modificació realitzada correctament.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Reduïm el segon comptador a 0 per avisar la Tasca 3 que pot procedir.
                latchModificat.countDown(); 
            }
        };

        // ==========================================
        // TASCA 3 (Callable): Retornar el saldo final
        // ==========================================
        // Utilitzem Callable<Double> en comptes de Runnable perquè ha de RETORNAR un resultat (Double)
        Callable<Double> tascaConsultaFinal = () -> {
            // El fil espera aquí fins que la Tasca 2 hagi aplicat els interessos i les comissions
            latchModificat.await(); 
            System.out.println("[Tasca 3] Processant consulta final per al client...");
            Thread.sleep(500); // Simula temps de generació de la consulta
            
            // Retorna el valor obtingut o 0.0 si la clau no existeix
            return compteBancari.getOrDefault("saldo", 0.0); 
        };

        // MOTOR D'EXECUCIÓ (POOLS DE FILS)
        // Creem un ExecutorService amb un grup fix de 3 fils. Requisit de l'exercici.
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            // Enviem les dues tasques de tipus Runnable al pool de fils mitjançant 'execute'
            executor.execute(tascaRecepcio);
            executor.execute(tascaInteressos);
            
            // Enviem la tasca Callable mitjançant 'submit'. 
            // Aquest mètode retorna un objecte 'Future' que és com un "rebut" que contindrà el resultat futur.
            Future<Double> resultatFutur = executor.submit(tascaConsultaFinal);

            // OBTENCIÓ DEL RESULTAT FINAL
            // El mètode .get() bloqueja el fil principal ('main') fins que el Callable hagi acabat 
            // de calcular i hagi retornat el valor.
            Double saldoFinal = resultatFutur.get();
            
            // Presentació final del resultat obtingut
            System.out.println("\n--- RESULTAT ENVIAT AL CLIENT ---");
            System.out.printf("Operació completada. Saldo final: %.2f EUR\n", saldoFinal);
            System.out.println("---------------------------------");

        } catch (Exception e) {
            // Captura qualsevol error d'interrupció o d'execució de les tasques
            System.err.println("Error en l'execució: " + e.getMessage());
        } finally {
            
            // TANQUEM L'EXECUTOR PER ALLIBERAR RECURSOS
            // Si no es tanca explicitament l'ExecutorService, els fils es queden actius a la memòria 
            // de la CPU esperant feina nova i el programa no finalitzaria mai.
            System.out.println("\nTancant el servei d'execució...");
            executor.shutdown(); // Demana un tancament ordenat (no accepta noves tasques)
            
            try {
                // Donem un marge de 5 segons per tancar de forma neta
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Si passa el temps, força el tancament immediat
                }
                System.out.println("Executor tancat correctament.");
            } catch (InterruptedException e) {
                executor.shutdownNow(); // Força el tancament si el fil principal és interromput
            }
        }
    }
}
