### Grails Discord Plugin v1.0.0 
<br>
Easy Integration With Discord

Simply Add Token to the Config File

v1.0.0 Compatible with Grails 3
```yml 
discord.bot.token: "YOUR TOKEN"
```
Using Javacord as core api <br>
https://github.com/Javacord/Javacord

Then you can use the following code to send a message

By inject the DiscordService into your code
```Groovy
def MessageService
{
    DiscordService discordService
    
    void sendMessage(String message)
    {
        def channelId = 123456789
        discordService.sendMessageToChannel(channelId, message)
    }
}
```

Can also send to a user or using webhook 
```Groovy
def MessageService
{
    DiscordService discordService
    
    void sendMessage(String message)
    {
        def userId = 123456789
        discordService.sendMessageToChannel(channelId, message)
        
        def webhookUrl = "https://discord.com/api/webhooks/123456789/123456789"
        discordService.sendMessageToWebhook(webhookUrl, message)
    }
}
```

To build a more beautiful message, you can use MessageBuilder for more customization
```Groovy
def embed = new EmbedBuilder()
                .setTitle("Embed Notification")
                .setDescription("Embed Message")
                .setColor(Color.RED)
                .setFooter("${new Date().format("yyyy-MM-dd HH:mm:ss:SSS")}")
                .setThumbnail("https://www.grails.org/images/grails_logo.png")
                .addField("Environment", "Staging", true)
                .addField("Mode", "Slave", true)
                .addField("Level", "Error", true)

def message = new MessageBuilder()
        .setEmbed(embed)
```

Please refer to Javacord documentation for more information
https://javacord.org/wiki/basic-tutorials/message-builder.html

