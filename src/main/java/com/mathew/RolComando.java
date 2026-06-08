package com.mathew;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

public class RolComando {

    private final Random random = new Random();
    
    private static final String ADMIN_ROLE_ID = "1478860052046807040"; 

    public void ejecutar(SlashCommandInteractionEvent event) {
        
        boolean esAdmin = event.getMember().getRoles().stream()
                .anyMatch(rol -> rol.getId().equals(ADMIN_ROLE_ID));

        if (!esAdmin) {
            EmbedBuilder embedError = new EmbedBuilder();
            embedError.setDescription("❌ ¡No puedes usar este comando, no eres admin!");
            embedError.setColor(new Color(239, 83, 80)); 
            
            event.replyEmbeds(embedError.build())
                    .setEphemeral(true) 
                    .queue();
            return;
        }

        String nombreRol = event.getOption("nombre").getAsString();
        Color colorRandom = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));

        event.getGuild().createRole()
                .setName(nombreRol)
                .setColor(colorRandom)
                .setPermissions(new ArrayList<>())
                .queue(
                    rolCreado -> {
                        EmbedBuilder embedExito = new EmbedBuilder();
                        embedExito.setDescription("🎖️ El Coronel ha creado el rol **" + rolCreado.getName() + "** con un color aleatorio.");
                        embedExito.setColor(colorRandom); 
                        
                        event.replyEmbeds(embedExito.build()).queue();
                    },
                    error -> {
                        System.out.println("❌ Fallo al crear rol: " + error.getMessage());
                        event.reply("❌ No pude crear el rol. Verifica que mi rol (CORONEL MUDKIP) esté arriba de todo en los Ajustes de Roles.")
                                .setEphemeral(true)
                                .queue();
                    }
                );
    }
}