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

    // 닉네임 업데이트
    private void saveNicknames() {
        try {
            JSONObject json = new JSONObject(nicknames);
            Files.write(nicknameFile.toPath(), json.toString(4).getBytes());
            getLogger().info("닉네임 데이터가 업데이트되었습니다.");
        } catch (IOException e) {
            getLogger().severe("nicknames.json 파일을 저장하는 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 닉네임 리로드 명령어
        if (command.getName().equalsIgnoreCase("nickreload")) {
            loadNicknames();
            sender.sendMessage(ChatColor.GREEN + "닉네임 데이터가 다시 로드되었습니다.");
            applyNicknamesToAllPlayers();
            return true;
        }

        // 닉네임 직접 설정 명령어 (/nickset [닉네임])
        if (command.getName().equalsIgnoreCase("nickset")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }
    
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "사용법: /nickset <닉네임>");
                return true;
            }
    
            Player player = (Player) sender;
            String playerID = player.getName();
            String newNickname = args[0];

                // 이미 닉네임이 설정된 경우 변경 불가능
            if (nicknames.containsKey(playerID)) {
                sender.sendMessage(ChatColor.RED + "이미 닉네임이 설정되어 있습니다! 닉네임을 변경하려면 관리자에게 문의하세요.");
                return true;
            }

            // 닉네임이 이미 존재하는지 확인
            if (nicknames.containsValue(newNickname)) {
                sender.sendMessage(ChatColor.RED + newNickname +"은 이미 사용 중인 닉네임입니다!");
                return true;
            }
    
            // 닉네임 설정 및 적용
            nicknames.put(playerID, newNickname);
            saveNicknames();
            sender.sendMessage(ChatColor.GREEN + "닉네임이 '" + newNickname + "'(으)로 설정되었습니다!");
            applyNicknamesToAllPlayers();;
            return true;
        }
        
        //
        if (command.getName().equalsIgnoreCase("nickadd")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "사용법: /nickadd <플레이어ID> <닉네임>");
                return true;
            }
        
            String playerID = args[0];
            String nickname = args[1];
        
            // 플레이어가 실제 존재하는지 확인
            if (!MojangAPI.doesPlayerExist(playerID)) {
                sender.sendMessage(ChatColor.RED + playerID + "는 존재하지 않는 마인크래프트 계정입니다!");
                return true;
            }
        
            if (nicknames.containsKey(playerID)) {
                sender.sendMessage(ChatColor.RED + playerID + "의 닉네임이 이미 존재합니다!");
                return true;
            }
        
            // 새 닉네임이 이미 존재하는지 확인
            if (nicknames.containsValue(nickname)) {
                sender.sendMessage(ChatColor.RED + nickname +"은(는) 이미 사용 중인 닉네임입니다!");
                return true;
            }
            

            nicknames.put(playerID, nickname);
            saveNicknames();
            sender.sendMessage(ChatColor.GREEN + playerID + "의 닉네임이 '" + nickname + "'으로 추가되었습니다!");
        
            // 즉시 적용
            Player player = Bukkit.getPlayer(playerID);
            if (player != null) {
                applyNicknamesToAllPlayers();
            }
        
            return true;
        }

        // 관리자용 닉네임 수정 명령어 (/nickmodify [닉네임 or 플레이어] [새 닉네임])
        if (command.getName().equalsIgnoreCase("nickmodify")) {
            if (!sender.hasPermission("nickname.manage")) {
                sender.sendMessage(ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                return true;
            }
    
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "사용법: /nickmodify <닉네임 or 플레이어> <새 닉네임>");
                return true;
            }
    
            String target = args[0];
            String newNickname = args[1];
    
            // 새 닉네임이 이미 존재하는지 확인
            if (nicknames.containsValue(newNickname)) {
                sender.sendMessage(ChatColor.RED + "이미 사용 중인 닉네임입니다!");
                return true;
            }
    
            boolean found = false;
    
            // 닉네임을 기준으로 변경
            for (Map.Entry<String, String> entry : nicknames.entrySet()) {
                if (entry.getValue().equals(target)) {
                    nicknames.put(entry.getKey(), newNickname);
                    found = true;
                    break;
                }
            }
    
            // 플레이어 이름을 기준으로 변경
            if (!found && nicknames.containsKey(target)) {
                nicknames.put(target, newNickname);
                found = true;
            }
    
            if (!found) {
                sender.sendMessage(ChatColor.RED + "해당 플레이어나 닉네임을 찾을 수 없습니다.");
                return true;
            }
    
            saveNicknames();
            sender.sendMessage(ChatColor.GREEN + target + "의 닉네임이 '" + newNickname + "'으로 변경되었습니다!");
    
            // 즉시 적용
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer != null) {
                applyNicknamesToAllPlayers();
            }
    
            return true;
        }

        // 닉네임 삭제 명령어
        if (command.getName().equalsIgnoreCase("nickdel")) {
            if (!sender.hasPermission("nickname.manage")) {
                sender.sendMessage(ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "사용법: /nickdel <플레이어ID 또는 닉네임>");
                return true;
            }

            String target = args[0];
            boolean found = false;

            // 플레이어 ID로 직접 삭제 시도
            if (nicknames.containsKey(target)) {
                nicknames.remove(target);
                found = true;
            } else {
                // 닉네임으로 플레이어 찾기
                String playerToRemove = null;
                for (Map.Entry<String, String> entry : nicknames.entrySet()) {
                    if (entry.getValue().equals(target)) {
                        playerToRemove = entry.getKey();
                        break; // 동일한 닉네임이 여러 개일 경우, 첫 번째만 삭제
                    }
                }

                if (playerToRemove != null) {
                    nicknames.remove(playerToRemove);
                    found = true;
                    target = playerToRemove; // 출력 메시지에 사용할 수 있도록 변경
                }
            }

            if (!found) {
                sender.sendMessage(ChatColor.RED + target + "의 닉네임이 존재하지 않습니다!");
                return true;
            }

            saveNicknames();
            sender.sendMessage(ChatColor.GREEN + target + "의 닉네임이 삭제되었습니다!");

            // 즉시 적용
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                applyNicknamesToAllPlayers();
            }

            return true;
        }            
        return false;
    }
}