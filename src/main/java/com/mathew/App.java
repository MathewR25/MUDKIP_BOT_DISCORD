package com.mathew;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData; // 🚨 IMPORTANTE: JDA necesita esto para las opciones múltiples
import net.dv8tion.jda.api.requests.GatewayIntent;

public class App {

    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");

        BotListener miEscuchador = new BotListener();

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(miEscuchador) 
                .build()
                .awaitReady(); 

        jda.updateCommands().queue();

        String idMiServidor = "1478859966365569085"; 

        jda.updateCommands().queue(comandsGlobalesBorrados -> {

            if (jda.getGuildById(idMiServidor) != null) {
                jda.getGuildById(idMiServidor).updateCommands().addCommands(
                        Commands.slash("crear-rol", "Crea un rol normal con color aleatorio (Solo Admins)")
                                .addOption(OptionType.STRING, "nombre", "Escribe el nombre del rol a crear", true),

                        Commands.slash("partido-fijar", "Sube un nuevo partido al mostrador (Usa: Emojis + Nombres)")
                                .addOptions(
                                    new OptionData(OptionType.STRING, "equipo_a", "Pon el escudo (emoji) y nombre del Equipo A", true),
                                    new OptionData(OptionType.STRING, "equipo_b", "Pon el escudo (emoji) y nombre del Equipo B", true)
                                ),

                        Commands.slash("partido-borrar", "Elimina un partido del mostrador usando su ID")
                                .addOption(OptionType.INTEGER, "id_partido", "El número de ID del partido que deseas borrar", true),

                        Commands.slash("apuesta-cerrar", "Cierra un partido, calcula la ecuación y reparte el pozo")
                                .addOptions(
                                    new OptionData(OptionType.INTEGER, "id_partido", "El número de ID del partido que terminó", true),
                                    new OptionData(OptionType.STRING, "resultado", "El resultado real del partido", true)
                                        .addChoice("Equipo A (Ganador)", "GANADOR_A")
                                        .addChoice("Equipo B (Ganador)", "GANADOR_B")
                                        .addChoice("Empate", "EMPATE")
                                        .addChoice("DF (Defeat)", "DF")
                                ),

                        Commands.slash("partidos", "Muestra la cartelera de partidos disponibles y sus pozos actuales"),

                        Commands.slash("apostar", "Abre tu panel secreto de apuestas con botones de escudos")
                                .addOptions(
                                    new OptionData(OptionType.INTEGER, "id_partido", "El número de ID del partido al que quieres apostar", true),
                                    new OptionData(OptionType.INTEGER, "monto", "Cantidad de monedas a apostar (Debe estar en tu banco)", true)
                                )
                ).queue(comandosServerListos -> {
                    System.out.println("----------------------------------------");
                    System.out.println("¡Limpieza global exitosa!");
                    System.out.println("¡Comandos únicos inyectados en el servidor con éxito!");
                    System.out.println("----------------------------------------");
                });
            } else {
                System.out.println("⚠️ Error: El ID del servidor no es correcto o el bot no está dentro de él.");
            }
            
        });
        
        System.out.println("----------------------------------------");
        System.out.println("¡Un Mudkip salvaje ha aparecido!");
        System.out.println("----------------------------------------");
    }
}