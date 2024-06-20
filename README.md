# DC-Backup

DC-Backup is a Discord bot written in Java using Maven, designed to backup and restore Discord servers on command.

## Features
- Backup entire Discord server including channels, roles, and permissions.
- Restore backups to restore server configurations.

## Installation
1. Clone the repository from GitHub: https://github.com/Coho04/DC-Backup
2. Import the project into your favourite IDE
3. Run the mvn clean install command to install the required dependencies
4. Copy the .env.example file to an .env file in the root directory of the project and add your bot token and other configurations
5. Build and launch the bot

## Usage
- /backup - Creates a backup of the current server
- /import - Restores the server from the specified backup (Important: The last message must contain the backup file).
- /discord-delete - Clears the Discord Server
- /help - Displays the list of commands!
- /restart - Restart the Bot!
- /bot-stats - Shows you the bot statistics!
- /invite - Invite the bot to your server!
- /ping - Shows the latency of the bot!

## License
This project is licensed under the MIT license.

## Support
If you have problems or questions, you can open an issue on GitHub or contact the developer directly.

