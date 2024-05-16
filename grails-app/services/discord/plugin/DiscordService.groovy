package discord.plugin


import grails.util.Holders
import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.channel.Channel
import org.javacord.api.entity.channel.ChannelType
import org.javacord.api.entity.intent.Intent
import org.javacord.api.entity.message.MessageBuilder
import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.entity.message.component.ActionRow
import org.javacord.api.entity.message.component.Button
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.user.User
import org.javacord.api.entity.webhook.Webhook
import org.javacord.api.interaction.SlashCommand

import javax.annotation.PostConstruct
import java.awt.*
import java.util.concurrent.TimeUnit

class DiscordService {
    private DiscordApi api = null

    @PostConstruct
    def init() {
        def prefix = "[INFO]${new Date().format('yyyy-MM-dd HH:mm:ss:SSS')} - "
        String token = Holders.config.getRequiredProperty("discord.bot.token")
        if (token) {
            def discordApi = new DiscordApiBuilder().setToken(token).addIntents(Intent.MESSAGE_CONTENT, Intent.GUILD_MEMBERS).login();
            try {
                api = discordApi.join()
            } catch (Exception e) {
                e.printStackTrace()
            }
        } else {
            println("${prefix}Discord token is not provided")
        }

        if (api) {
            api.getServers().forEach { server ->
                println("${prefix}Connected to server: ${server.getName()}, Server ID: ${server.getId()}")
            }
            println("${prefix}Invite Link ": api.createBotInvite());
            //TODO send connection message to bot channel

            // add event listeners, please see the javadocs for a list of all available listeners
            api.addMessageCreateListener {event ->
                if (event.getMessageContent().equalsIgnoreCase("!ping")) {
                    event.messageAuthor.asUser().ifPresent {event.getChannel().sendMessage("Pong " + it.getMentionTag() + "!")}
                }
            }
//            addHelloEvent()
//            addAuthEvent()
//            // add comment
//            addCommand()
//            addCommandEvent()
        }
        return api
    }

    void stop() {
        if (api != null) {
            api.disconnect()
        }
    }

    void sendMessageToUser(String discordId, Object message) {
        if (!discordId) {
            System.out.println("Discord ID is not provided")
            return
        }
        sendMessageToUser(Long.parseLong(discordId), message)
    }

    void sendMessageToUser(Long discordId, Object message) {
        if (!api) {
            System.out.println("Discord API is not initialized")
            return
        }

        if (!discordId) {
            System.out.println("Discord ID is not provided")
            return
        }


        if (message instanceof String || message instanceof EmbedBuilder || message instanceof MessageBuilder) {
            api.getUserById(discordId).get(5, TimeUnit.SECONDS).openPrivateChannel().thenAccept { privateChannel ->
                privateChannel.sendMessage(message)
            }
        } else {
            System.out.println("Message type is not supported")
        }
    }

    void sendMessageToWebhook(String webhookUrl, MessageBuilder message) {
        if (!api) {
            System.out.println("Discord API is not initialized")
            return
        }

        if (!webhookUrl) {
            System.out.println("Webhook URL is not provided")
            return
        }

        Webhook webhook = api.getIncomingWebhookByUrl(webhookUrl).get()

        message.send(webhook).exceptionally { e ->
            e.printStackTrace()
        }
    }

    void sendMessageToChannel(String channelId, Object message) {
        if (!channelId) {
            System.out.println("Discord ID is not provided")
            return
        }
        sendMessageToChannel(Long.parseLong(channelId), message)
    }

    void sendMessageToChannel(Long channelId, Object message) {
        if (!api) {
            System.out.println("Discord API is not initialized")
        }

        if (!channelId) {
            System.out.println("Discord ID is not provided")
            return
        }

        def channel = api.getChannelById(channelId).get()

        if (message instanceof String)  {

            if (channel.type == ChannelType.SERVER_TEXT_CHANNEL) {
                channel.asTextChannel().ifPresent { textChannel ->
                    textChannel.sendMessage(message)
                }
            }
        }
        else if (message instanceof EmbedBuilder) {
            channel.asTextChannel().ifPresent { textChannel ->
                textChannel.sendMessage(message)
            }
        }
        else if (message instanceof MessageBuilder) {
            channel.asTextChannel().ifPresent { textChannel ->
            }
        }
        else {
            System.out.println("Message type is not supported")
        }
    }

    void sendMessageToChannel(String channelId, String title, String content, Color color = Color.DARK_GRAY) {
        if (!channelId) {
            System.out.println("Discord ID is not provided")
            return
        }
        sendMessageToChannel(Long.parseLong(channelId), title, content, color)
    }

    void sendMessageToChannel(Long channelId, String title, String content, Color color = Color.DARK_GRAY) {
        if (!api) {
            System.out.println("Discord API is not initialized")
        }

        if (!channelId) {
            System.out.println("Discord ID is not provided")
            return
        }

        def message = new EmbedBuilder().setTitle(title).setDescription(content).setColor(color)

        def channel = api.getChannelById(channelId).get()

        if (channel.type == ChannelType.SERVER_TEXT_CHANNEL) {
            channel.asTextChannel().ifPresent { textChannel ->
                textChannel.sendMessage(message)
            }
        }
    }

    Channel getChannel(String channelId) {
        return api.getChannelById(channelId).get()
    }

    Channel getChannel(Long channelId) {
        return api.getChannelById(channelId).get()
    }

    User getUser(String userId) {
        return api.getUserById(userId).get()
    }

    User getUser(Long userId) {
        return api.getUserById(userId).get()
    }

    // test event
    private void addHelloEvent() {
        api.addMessageCreateListener {event ->
            if (event.getMessageContent().equalsIgnoreCase("!hello")) {
                event.messageAuthor.asUser().ifPresent {event.getChannel().sendMessage("Hello " + it.getMentionTag() + "!")}
            }
        }
    }

    private void addAuthEvent() {
        api.addMessageCreateListener {event ->
            if (event.getMessageContent().equalsIgnoreCase("!Auth")) {
                println("Get Auth Message")
                event.getMessageAuthor().asUser().ifPresent {
                    def message = authenticateMessage(it.id)

                    message.send(event.getChannel()).exceptionally { e ->
                        e.printStackTrace()
                        null
                    }
                }
            }
        }

        api.addButtonClickListener { event ->
            // get event author
            def interaction = event.getButtonInteraction()
            def customId = interaction.getCustomId()

            if (customId.startsWith("auth-")) {
                def userId = customId.split("-")[1]
                event.getButtonInteraction().createImmediateResponder().setContent("You have clicked the button!").respond()

                def updater = interaction.getMessage().createUpdater()
                updater.removeAllComponents()
                updater.addComponents(ActionRow.of(Button.secondary("auth-waiting", "Authorizing...", true)))
                updater.applyChanges()
            }
        }
    }

    private addCommandEvent() {
        api.addSlashCommandCreateListener { event ->
            if (event.getSlashCommandInteraction().getCommandName() == "pingbot") {
                event.getSlashCommandInteraction().createImmediateResponder().setContent("Pong!").setFlags(MessageFlag.EPHEMERAL).respond()
            }
        }
    }

    def authenticateMessage(Long userId) {
        def message = new MessageBuilder()
        def embed = new EmbedBuilder()
        embed.setTitle("Authroization")
        embed.setDescription("Please Click The Button To Authorize Your Account!")
        embed.setColor(Color.RED)
        message.addEmbed(embed)
        def link = Button.link("http://dev-erp.fontrip.com:8080/auth/${userId}", 'Authorize', '🔑')
//        def select = SelectMenu.createStringMenu('food', '最愛的食物', [option, option2, option3] as List)
        message.addComponents(ActionRow.of(link))
        return message
    }

    def addCommand() {
        SlashCommand command = SlashCommand.with("pingbot", "Checks the functionality of this command")
                                .createGlobal(api)
                                .join();
    }
}
