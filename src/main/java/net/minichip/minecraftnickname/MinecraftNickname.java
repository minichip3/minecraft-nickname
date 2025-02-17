package net.minichip.minecraftnickname;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class MinecraftNickname extends JavaPlugin implements Listener {
    private Map<String, String> nicknames = new HashMap<>();
    private File configFile;
    private File nicknameFile;

    @Override
    public void onEnable() {
        getLogger().info("MinecraftNickname Plugin이 활성화되었습니다!");

        // 플러그인 폴더 생성
        File pluginDir = getDataFolder();
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }

        // config.yml 및 nicknames.json 파일 설정
        configFile = new File(pluginDir, "config.yml");

        if (!configFile.exists()) {
            createDefaultConfig();
        }

        loadConfig();
        loadNicknames();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nickreload").setExecutor(this);

        // 서버에 접속 중인 모든 플레이어의 닉네임 적용
        applyNicknamesToAllPlayers();
    }

    @Override
    public void onDisable() {
        getLogger().info("MinecraftNickname Plugin이 비활성화되었습니다.");
    }

    private void createDefaultConfig() {
        try {
            getLogger().warning("config.yml 파일이 존재하지 않습니다. 기본 파일을 생성합니다.");
            configFile.createNewFile();

            String defaultConfig = "# 닉네임 JSON 파일의 경로를 설정하세요.\n"
                    + "# 절대경로 사용 가능: 예) \"/minecraft/nicknames.json\"\n"
                    + "# 설정을 하지 않으면 기본값 \"nicknames.json\"이 사용됩니다.\n"
                    + "#\n"
                    + "# 예시:\n"
                    + "# nickname-file-path: \"nicknames.json\"\n";

            Files.write(configFile.toPath(), defaultConfig.getBytes());
        } catch (IOException e) {
            getLogger().severe("config.yml 파일 생성 중 오류 발생: " + e.getMessage());
        }
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            getLogger().warning("config.yml 파일을 찾을 수 없습니다!");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String filePath = config.getString("nickname-file-path", "nicknames.json");

        if (filePath.startsWith("/") || filePath.contains(":")) {
            nicknameFile = new File(filePath);
        } else {
            nicknameFile = new File(getDataFolder(), filePath);
        }

        getLogger().info("닉네임 JSON 파일 경로: " + nicknameFile.getAbsolutePath());
    }

    private void loadNicknames() {
        if (!nicknameFile.exists()) {
            getLogger().warning("nicknames.json 파일이 존재하지 않습니다. 빈 파일을 생성합니다.");
            try {
                nicknameFile.createNewFile();
                Files.write(nicknameFile.toPath(), "{}".getBytes());
            } catch (IOException e) {
                getLogger().severe("nicknames.json 파일 생성 중 오류 발생: " + e.getMessage());
            }
        }

        try {
            String content = new String(Files.readAllBytes(nicknameFile.toPath()));
            JSONObject json = new JSONObject(content);
            nicknames.clear();
            for (String key : json.keySet()) {
                nicknames.put(key, json.getString(key));
            }
            getLogger().info("닉네임 데이터 로드 완료: " + nicknames);
        } catch (IOException e) {
            getLogger().severe("nicknames.json 파일을 읽는 중 오류 발생: " + e.getMessage());
        }
    }

    // 채팅 메시지에서 닉네임 적용
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerName = event.getPlayer().getName();
        String nickname = nicknames.get(playerName);
        String message = event.getMessage().replace("%", "%%");

        if (nickname != null) {
            event.setFormat(ChatColor.AQUA + nickname + " " + ChatColor.GOLD + "<" + playerName + "> " + ChatColor.WHITE + message);
        } else {
            event.setFormat(ChatColor.GOLD + "<" + playerName + "> " + ChatColor.WHITE + message);
        }
    }

    // 닉네임을 머리 위, 탭 리스트에 즉시 적용
    private void applyNicknamesToAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyNicknameToPlayer(player);
        }
    }

    private void applyNicknameToPlayer(Player player) {
        String playerName = player.getName();
        String nickname = nicknames.get(playerName);

        if (nickname != null) {
            // 탭 리스트 적용
            player.setPlayerListName(ChatColor.AQUA + "[" + nickname + "] " + ChatColor.RESET + playerName);

            // 팀을 사용하여 머리 위 닉네임 적용
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(playerName);

            if (team == null) {
                team = scoreboard.registerNewTeam(playerName);
            } else {
                team.unregister();
                team = scoreboard.registerNewTeam(playerName);
            }

            team.setPrefix(ChatColor.AQUA + "[" + nickname + "] " + ChatColor.RESET);
            team.addEntry(player.getName());
        } else {
            // 닉네임이 없는 경우 기본값 적용
            player.setPlayerListName(ChatColor.RESET + playerName);

            // 팀 제거 (머리 위 닉네임 삭제)
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(playerName);
            if (team != null) {
                team.unregister();
            }
        }
    }

    // 닉네임 다시 불러오는 명령어 (/nickreload)
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("nickreload")) {
            if (sender.hasPermission("nickname.reload") || sender.isOp()) {
                loadNicknames();
                sender.sendMessage(ChatColor.GREEN + "닉네임 데이터가 다시 로드되었습니다.");

                // 모든 플레이어의 닉네임을 즉시 적용
                applyNicknamesToAllPlayers();
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                return false;
            }
        }
        return false;
    }
}