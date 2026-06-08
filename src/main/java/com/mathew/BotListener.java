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
import java.io.File;
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

        // Comando Crear Rol Intacto
        if (nombreComando.equals("crear-rol")) {
            rolComando.ejecutar(event);
        }

        // 🏟️ COMANDO ADMIN: FIJAR PARTIDO
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

        // 🗑️ COMANDO ADMIN: BORRAR PARTIDO (CON REEMBOLSO AUTOMÁTICO)
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

            // Devolución automática mediante API a UnbelievaBoat
            for (Apuesta ap : partidoEncontrado.getApuestas()) {
                boolean devuelto = unbelieva.modificarSaldo(ap.getUsuarioId(), ap.getCantidad(), "Reembolso: Partido #" + idBuscar + " Cancelado");
                if (devuelto) {
                    System.out.println("✅ [Mudkip Reembolso] Devolviendo " + ap.getCantidad() + " monedas al usuario " + ap.getUsuarioId() + " mediante API Token.");
                } else {
                    System.out.println("❌ [Mudkip Reembolso] Falló la devolución al usuario " + ap.getUsuarioId());
                }
            }

            listaPartidos.remove(partidoEncontrado);

            EmbedBuilder embedBorrar = new EmbedBuilder()
                    .setTitle("🗑️ PARTIDO CANCELADO / BORRADO")
                    .setDescription("El partido con **ID #" + idBuscar + "** ha sido eliminado del mostrador.\n\n" +
                                    "📢 **ATENCIÓN:** Se ha procesado el reembolso completo de forma automática en UnbelievaBoat a todas las cuentas que apostaron en este encuentro.")
                    .setColor(Color.ORANGE);

            event.replyEmbeds(embedBorrar.build()).queue();
        }

        // 🏁 COMANDO ADMIN: CERRAR APUESTA Y DISTRIBUIR POZO EN ENTEROS PUROS (MODIFICADO Y BLINDADO)
        if (nombreComando.equals("apuesta-cerrar")) {
            int idBuscar = event.getOption("id_partido").getAsInt();
            // Normalizamos el texto ingresado por el administrador eliminando espacios vacíos externos
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

            // 1. Recalculamos los pozos leyendo los datos crudos para evitar desajustes de tipos de datos
            for (Apuesta ap : partidoEncontrado.getApuestas()) {
                String opcionApuesta = ap.getOpcion().toUpperCase();
                pozoTotal += ap.getCantidad();
                
                // Comparación flexible bidireccional (.contains) para emparejar por ejemplo "DF" con "GANADOR_DF"
                if (opcionApuesta.contains(resultadoAdmin) || resultadoAdmin.contains(opcionApuesta)) {
                    pozoGanador += ap.getCantidad();
                }
            }

            // Si nadie apostó a la opción ganadora
            if (pozoGanador == 0) {
                listaPartidos.remove(partidoEncontrado);
                event.reply("🏁 Partido #" + idBuscar + " cerrado. Nadie apostó por la opción ganadora `" + resultadoAdmin + "`. El pozo de " + MANGO + " **" + pozoTotal + "** se ha perdido.").queue();
                return;
            }

            // 2. Distribución proporcional exacta del premio total acumulado
            int ganadoresPagados = 0;
            for (Apuesta ap : partidoEncontrado.getApuestas()) {
                String opcionApuesta = ap.getOpcion().toUpperCase();
                
                if (opcionApuesta.contains(resultadoAdmin) || resultadoAdmin.contains(opcionApuesta)) {
                    long apuestaUsuario = ap.getCantidad();
                    
                    // 🧮 FÓRMULA PROPORCIONAL MULTI-MILLONARIA PROTEGIDA:
                    // Usamos variables de punto flotante de 64 bits (double) intermedios para prevenir un desbordamiento matemático (overflow)
                    double calculoProporcional = ((double) apuestaUsuario * (double) pozoTotal) / (double) pozoGanador;
                    long premioFinal = (long) calculoProporcional;

                    // Enviamos el premio total completo al Banco usando la nueva estructura String de UnbelievaClient
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

        // 🏟️ COMANDO PÚBLICO: VER CARTELERA DE PARTIDOS
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

        // 💰 COMANDO PÚBLICO: APOSTAR (DESPLIEGA PANEL SECO INTERACTIVO)
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
            } catch (Exception e) {
                // Conservar balones por defecto
            }

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
            String opcionSeleccionada;
            int idPartido;
            long monto;

            if (partes[1].equals("EMPATE") || partes[1].equals("DF")) {
                opcionSeleccionada = "GANADOR_" + partes[1];
                idPartido = Integer.parseInt(partes[2]);
                monto = Long.parseLong(partes[3]);
            } else {
                opcionSeleccionada = "GANADOR_" + partes[1]; 
                idPartido = Integer.parseInt(partes[2]);
                monto = Long.parseLong(partes[3]);
            }

            if (monto < 100) {
                event.reply("❌ ¡La apuesta mínima es de **100** " + MANGO + "! Por favor genera un panel nuevo.").setEphemeral(true).queue();
                return;
            }

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
                    event.reply("❌ ¡Ya registraste una apuesta en este encuentro! Solo se permite una apuesta por persona para cada partido.")
                            .setEphemeral(true).queue();
                    return;
                }
            }

            long saldoTotal = 0;
            try {
                String url = "https://unbelievaboat.com/api/v1/guilds/" + SERVER_ID + "/users/" + usuarioId;
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .header("Authorization", UNBELIEVA_TOKEN)
                        .header("Accept", "application/json")
                        .get()
                        .build();

                okhttp3.OkHttpClient clientTmp = new okhttp3.OkHttpClient();
                try (okhttp3.Response response = clientTmp.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        if (body.contains("\"total\":")) {
                            String sub = body.substring(body.indexOf("\"total\":") + 8);
                            String val = sub.split("[,}]")[0].trim();
                            saldoTotal = (long) Double.parseDouble(val.replace("\"", ""));
                        }
                    }
                }
            } catch (Exception e) {
                saldoTotal = unbelieva.getBankBalance(usuarioId);
            }

            if (saldoTotal < monto) {
                event.reply("❌ ¡" + MANGO + " insuficientes! Tu saldo Total actual es de " + MANGO + " **" + saldoTotal + "** y estás intentando apostar **" + monto + "**.")
                        .setEphemeral(true).queue();
                return;
            }

            boolean cobroExitoso = unbelieva.modificarSaldo(usuarioId, -monto, "Apuesta Partido #" + idPartido);
            if (!cobroExitoso) {
                event.reply("❌ Hubo un error de comunicación con UnbelievaBoat. Inténtalo de nuevo.").setEphemeral(true).queue();
                return;
            }

            Apuesta nuevaApuesta = new Apuesta(usuarioId, monto, opcionSeleccionada);
            partido.getApuestas().add(nuevaApuesta);
            partido.agregarMonedasAlPozo(monto);

            event.reply("✅ ¡Apuesta procesada con éxito!\nHas jugado **" + monto + "** " + MANGO + " a la opción `" + opcionSeleccionada + "` para el Partido #" + idPartido + ".")
                    .setEphemeral(true).queue();
            
            System.out.println("🤖 [Mudkip] Jugador " + event.getUser().getName() + " metió " + monto + " monedas al partido #" + idPartido);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser()) 
                && event.getChannel().getId().equals(CANAL_GENERAL_ID)) {
            
            File gif1 = new File("gifs/mudkip (1).gif");
            File gif2 = new File("gifs/mudkip.gif");
            File gif3 = new File("gifs/pokemon-mudkip.gif");
            File[] listaGifs = {gif1, gif2, gif3};

            int indiceAleatorio = random.nextInt(listaGifs.length);
            File gifSeleccionado = listaGifs[indiceAleatorio];

            if (!gifSeleccionado.exists()) {
                System.out.println("⚠️ [Mudkip] Archivo NO encontrado en la ruta: " + gifSeleccionado.getAbsolutePath());
                return; 
            }

            System.out.println("🤖 [Mudkip] Intentando enviar: " + gifSeleccionado.getName());

            event.getChannel().sendTyping().queue();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setDescription("**¡MUDKI MUDKIP!!!** 🌊"); 
            embed.setColor(new Color(173, 230, 250)); 
            
            String nombreAdjunto = "mudkip_animado.gif";
            embed.setImage("attachment://" + nombreAdjunto); 

            FileUpload fileUpload = FileUpload.fromData(gifSeleccionado, nombreAdjunto);

            event.getChannel().sendMessageEmbeds(embed.build())
                    .addFiles(fileUpload)
                    .queue(
                        exito -> System.out.println("✅ [Mudkip] ¡GIF enviado con éxito!"),
                        error -> {
                            System.out.println("❌ [Mudkip] Discord rechazó el envío: " + error.getMessage());
                            event.getChannel().sendMessage("¡Mudkip se resbaló con el archivo! 🌊 (Error de red)").queue();
                        }
                    );
        }
    }
}
