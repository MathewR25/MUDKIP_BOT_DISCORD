package com.mathew;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotListener extends ListenerAdapter {

    private final Random random = new Random();
    private static final String CANAL_GENERAL_ID = "1478859967284117607"; 
    private final RolComando rolComando = new RolComando();

    private final String ID_ROL_ADMIN = "1478860052046807040"; 
    private final String UNBELIEVA_TOKEN = System.getenv("UNBELIEVA_TOKEN");
    private final String SERVER_ID = "1478859966365569085";
    private final UnbelievaClient unbelieva = new UnbelievaClient(UNBELIEVA_TOKEN, SERVER_ID);
    
    private final String MANGO = "<:mango:1499083166831480903>"; 
    
    private final List<Partido> listaPartidos = new ArrayList<>();
    private int contadorIdPartido = 1;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String nombreComando = event.getName();

        if (nombreComando.equals("crear-rol") || nombreComando.equals("partido-fijar") || nombreComando.equals("partido-borrar") || nombreComando.equals("apuesta-cerrar")) {
            Role rolAdmin = event.getGuild().getRoleById(ID_ROL_ADMIN);
            if (rolAdmin == null || !event.getMember().getRoles().contains(rolAdmin)) {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("⛔ Acceso Denegado")
                        .setDescription("NO ERES ADMIN NO PUEDES USAR ESTE COMANDO, MUDKIP MUDKIP")
                        .setColor(Color.RED);
                event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                return; 
            }
        }

        if (nombreComando.equals("crear-rol")) {
            rolComando.ejecutar(event);
        }

        if (nombreComando.equals("partido-fijar")) {
            String equipoA = event.getOption("equipo_a").getAsString();
            String equipoB = event.getOption("equipo_b").getAsString();

            Partido nuevoPartido = new Partido(contadorIdPartido, equipoA, equipoB);
            listaPartidos.add(nuevoPartido);

            EmbedBuilder embedFijar = new EmbedBuilder()
                    .setTitle("🏟️ ¡NUEVO PARTIDO REGISTRADO!")
                    .setDescription("El administrador ha subido un encuentro al mostrador.\n\n" +
                                    "🆔 **ID del Partido:** " + contadorIdPartido + "\n" +
                                    "⚔️ **Encuentro:** " + equipoA + " **VS** " + equipoB + "\n\n" +
                                    " Use `/apostar` ingresando este ID para abrir tu panel de apuestas.")
                    .setColor(new Color(173, 230, 250))
                    .setFooter("Coronel Mudkip - Sistema de Control");

            event.replyEmbeds(embedFijar.build()).setEphemeral(true).queue();
            System.out.println("🤖 [Mudkip] Partido #" + contadorIdPartido + " creado: " + equipoA + " vs " + equipoB);
            contadorIdPartido++;
        }

        if (nombreComando.equals("partido-borrar")) {
            int idBuscar = event.getOption("id_partido").getAsInt();
            Partido partidoEncontrado = null;

            for (Partido p : listaPartidos) {
                if (p.getId() == idBuscar) {
                    partidoEncontrado = p;
                    break;
                }
            }

            if (partidoEncontrado == null) {
                event.reply("❌ No se encontró ningún partido activo con el ID #" + idBuscar).setEphemeral(true).queue();
                return;
            }

            for (Apuesta ap : partidoEncontrado.getApuestas()) {
                boolean devuelto = unbelieva.modificarSaldo(ap.getUsuarioId(), ap.getCantidad(), "Reembolso: Partido #" + idBuscar + " Cancelado");
                if (devuelto) {
                    System.out.println("✅ [Mudkip Reembolso] Devolviendo " + ap.getCantidad() + " monedas al usuario " + ap.getUsuarioId() + " mediante API Token.");
                } else {
                    System.out.println("❌ [Mudkip Reembolso] Falló la devolución al usuario " + ap.getUsuarioId());
                }
            }

            listaPartidos.remove(partidoEncontrado);
            
            // --- CAMBIO AQUÍ: REINICIO DE ID ---
            if (listaPartidos.isEmpty()) contadorIdPartido = 1;

            EmbedBuilder embedBorrar = new EmbedBuilder()
                    .setTitle("🗑️ PARTIDO CANCELADO / BORRADO")
                    .setDescription("El partido con **ID #" + idBuscar + "** ha sido eliminado del mostrador.\n\n" +
                                    "📢 **ATENCIÓN:** Se ha procesado el reembolso completo de forma automática en UnbelievaBoat a todas las cuentas que apostaron en este encuentro.")
                    .setColor(Color.ORANGE);

            event.replyEmbeds(embedBorrar.build()).queue();
        }

        if (nombreComando.equals("apuesta-cerrar")) {
            int idBuscar = event.getOption("id_partido").getAsInt();
            String resultadoAdmin = event.getOption("resultado").getAsString().toUpperCase().trim(); 
            Partido partidoEncontrado = null;

            for (Partido p : listaPartidos) {
                if (p.getId() == idBuscar) {
                    partidoEncontrado = p;
                    break;
                }
            }

            if (partidoEncontrado == null) {
                event.reply("❌ No se encontró ningún partido activo con el ID #" + idBuscar).setEphemeral(true).queue();
                return;
            }

            long pozoTotal = 0;
            long pozoGanador = 0;

            for (Apuesta ap : partidoEncontrado.getApuestas()) {
                String opcionApuesta = ap.getOpcion().toUpperCase();
                pozoTotal += ap.getCantidad();
                if (opcionApuesta.contains(resultadoAdmin) || resultadoAdmin.contains(opcionApuesta)) {
                    pozoGanador += ap.getCantidad();
                }
            }

            if (pozoGanador == 0) {
                listaPartidos.remove(partidoEncontrado);
                // --- CAMBIO AQUÍ ---
                if (listaPartidos.isEmpty()) contadorIdPartido = 1;

                event.reply("🏁 Partido #" + idBuscar + " cerrado. Nadie apostó por la opción ganadora `" + resultadoAdmin + "`. El pozo de " + MANGO + " **" + pozoTotal + "** se ha perdido.").queue();
                return;
            }

            int ganadoresPagados = 0;
            for (Apuesta ap : partidoEncontrado.getApuestas()) {
                String opcionApuesta = ap.getOpcion().toUpperCase();
                if (opcionApuesta.contains(resultadoAdmin) || resultadoAdmin.contains(opcionApuesta)) {
                    long apuestaUsuario = ap.getCantidad();
                    double calculoProporcional = ((double) apuestaUsuario * (double) pozoTotal) / (double) pozoGanador;
                    long premioFinal = (long) calculoProporcional;

                    boolean pagoExitoso = unbelieva.modificarSaldo(ap.getUsuarioId(), premioFinal, "Premio Proporcional Partido #" + idBuscar);
                    if (pagoExitoso) {
                        System.out.println("💰 [Mudkip Premio] Distribuidos " + premioFinal + " mangos totales a: " + ap.getUsuarioId());
                        ganadoresPagados++;
                    } else {
                        System.out.println("❌ [Mudkip Premio] Error al procesar pago web a: " + ap.getUsuarioId());
                    }
                }
            }

            listaPartidos.remove(partidoEncontrado);
            // --- CAMBIO AQUÍ ---
            if (listaPartidos.isEmpty()) contadorIdPartido = 1;

            EmbedBuilder embedCierre = new EmbedBuilder()
                    .setTitle("🏁 ¡PARTIDO CERRADO Y POZO DISTRIBUIDO!")
                    .setDescription("El Partido con **ID #" + idBuscar + "** ha finalizado con éxito.\n\n" +
                                    "✨ **Opción Ganadora:** `" + resultadoAdmin + "`\n" +
                                    "💰 **Pozo Total Acumulado:** " + MANGO + " **" + pozoTotal + "**\n" +
                                    "👥 **Ganadores premiados:** " + ganadoresPagados + " usuarios.\n\n" +
                                    "¡Los " + MANGO + " proporcionales han sido depositados de forma automática en UnbelievaBoat!")
                    .setColor(Color.GREEN)
                    .setFooter("Coronel Mudkip - Distribución Sin Céntimos");

            event.replyEmbeds(embedCierre.build()).queue();
        }

        if (nombreComando.equals("partidos")) {
            if (listaPartidos.isEmpty()) {
                EmbedBuilder embedVacio = new EmbedBuilder()
                        .setTitle("🏟️ Mostrador Vacío")
                        .setDescription("Actualmente no hay partidos disponibles en el mostrador.\n¡Vuelve más tarde para realizar tus apuestas!")
                        .setColor(Color.ORANGE)
                        .setFooter("Coronel Mudkip - Sistema de Control");
                event.replyEmbeds(embedVacio.build()).queue();
                return;
            }

            EmbedBuilder embedCartelera = new EmbedBuilder()
                    .setTitle("🏟️ MOSTRADOR DE PARTIDOS DISPONIBLES 🏟️")
                    .setColor(new Color(173, 230, 250))
                    .setFooter("Usa /apostar [id] [monto] para jugar en tu panel interactivo privado.");

            for (Partido p : listaPartidos) {
                embedCartelera.addField(
                    "🆔 Partido ID: " + p.getId(),
                    "⚔️ " + p.getEquipoA() + " **VS** " + p.getEquipoB() + "\n💰 **Pozo actual:** " + MANGO + " **" + ((long) p.getPozoTotal()) + "**\n",
                    false
                );
            }
            event.replyEmbeds(embedCartelera.build()).queue();
        }

        if (nombreComando.equals("apostar")) {
            int idBuscar = event.getOption("id_partido").getAsInt();
            long monto = event.getOption("monto").getAsLong();
            if (monto < 100) {
                event.reply("❌ ¡La apuesta mínima permitida es de **100** " + MANGO + "! Por favor ingresa una cantidad válida.").setEphemeral(true).queue();
                return;
            }

            Partido partidoEncontrado = null;
            for (Partido p : listaPartidos) {
                if (p.getId() == idBuscar) {
                    partidoEncontrado = p;
                    break;
                }
            }

            if (partidoEncontrado == null) {
                event.reply("❌ No se encontró ningún partido con el ID #" + idBuscar).setEphemeral(true).queue();
                return;
            }

            EmbedBuilder embedPanel = new EmbedBuilder()
                    .setTitle("🏟️ Panel de Confirmación de Apuesta")
                    .setDescription("Encuentro: **" + partidoEncontrado.getEquipoA() + " VS " + partidoEncontrado.getEquipoB() + "**\n" +
                                    "💰 Monto asignado: **" + monto + "** " + MANGO + "\n\n" +
                                    " *Selecciona tu opción favorita presionando los botones compactos de abajo:*")
                    .setColor(new Color(173, 230, 250));

            Emoji emojiA = Emoji.fromUnicode("⚽");
            Emoji emojiB = Emoji.fromUnicode("⚽");

            try {
                if (partidoEncontrado.getEquipoA().contains("<")) {
                    String rawEmoji = partidoEncontrado.getEquipoA().substring(partidoEncontrado.getEquipoA().indexOf("<"), partidoEncontrado.getEquipoA().indexOf(">") + 1);
                    emojiA = Emoji.fromFormatted(rawEmoji);
                }
                if (partidoEncontrado.getEquipoB().contains("<")) {
                    String rawEmoji = partidoEncontrado.getEquipoB().substring(partidoEncontrado.getEquipoB().indexOf("<"), partidoEncontrado.getEquipoB().indexOf(">") + 1);
                    emojiB = Emoji.fromFormatted(rawEmoji);
                }
            } catch (Exception e) {}

            event.replyEmbeds(embedPanel.build())
                    .setEphemeral(true)
                    .addActionRow(
                        Button.success("AP_A_" + idBuscar + "_" + monto, emojiA),
                        Button.danger("AP_B_" + idBuscar + "_" + monto, emojiB),
                        Button.primary("AP_EMPATE_" + idBuscar + "_" + monto, "Empate 🤝"),
                        Button.secondary("AP_DF_" + idBuscar + "_" + monto, "DF 💀")
                    ).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String idBoton = event.getComponentId();
        if (idBoton.startsWith("AP_")) {
            String[] partes = idBoton.split("_");
            String opcionSeleccionada = "GANADOR_" + partes[1];
            int idPartido = Integer.parseInt(partes[2]);
            long monto = Long.parseLong(partes[3]);

            Partido partido = null;
            for (Partido p : listaPartidos) {
                if (p.getId() == idPartido) {
                    partido = p;
                    break;
                }
            }

            if (partido == null) {
                event.reply("❌ Error: Este partido ya no se encuentra disponible en la cartelera.").setEphemeral(true).queue();
                return;
            }

            String usuarioId = event.getUser().getId();
            for (Apuesta ap : partido.getApuestas()) {
                if (ap.getUsuarioId().equals(usuarioId)) {
                    event.reply("❌ ¡Ya registraste una apuesta en este encuentro! Solo se permite una apuesta por persona para cada partido.").setEphemeral(true).queue();
                    return;
                }
            }

            boolean cobroExitoso = unbelieva.modificarSaldo(usuarioId, -monto, "Apuesta Partido #" + idPartido);
            if (cobroExitoso) {
                partido.getApuestas().add(new Apuesta(usuarioId, monto, opcionSeleccionada));
                partido.agregarMonedasAlPozo(monto);
                event.reply("✅ ¡Apuesta procesada con éxito!\nHas jugado **" + monto + "** " + MANGO + " a la opción `" + opcionSeleccionada + "` para el Partido #" + idPartido + ".").setEphemeral(true).queue();
                System.out.println("🤖 [Mudkip] Jugador " + event.getUser().getName() + " metió " + monto + " monedas al partido #" + idPartido);
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser()) && event.getChannel().getId().equals(CANAL_GENERAL_ID)) {
            String[] listaGifs = {"gifs/mudkip (1).gif", "gifs/mudkip.gif", "gifs/pokemon-mudkip.gif"};
            int indiceAleatorio = random.nextInt(listaGifs.length);
            String gifSeleccionado = listaGifs[indiceAleatorio];

            java.io.InputStream streamGif = getClass().getClassLoader().getResourceAsStream(gifSeleccionado);
            if (streamGif == null) {
                event.getChannel().sendMessage("¡Mudkip no encuentra sus animaciones en el servidor! 🌊").queue();
                return; 
            }

            event.getChannel().sendTyping().queue();
            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription("**¡MUDKI MUDKIP!!!** 🌊")
                    .setColor(new Color(173, 230, 250))
                    .setImage("attachment://mudkip_animado.gif");

            event.getChannel().sendMessageEmbeds(embed.build())
                    .addFiles(FileUpload.fromData(streamGif, "mudkip_animado.gif"))
                    .queue(exito -> System.out.println("✅ [Mudkip] ¡GIF enviado con éxito desde recursos!"), error -> System.out.println("❌ [Mudkip] Discord rechazó el envío: " + error.getMessage()));
        }
    }
}
