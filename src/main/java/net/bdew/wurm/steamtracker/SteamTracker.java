package net.bdew.wurm.steamtracker;

import com.wurmonline.server.creatures.Communicator;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.sql.SQLException;
import java.util.logging.Logger;

public class SteamTracker implements WurmServerMod, Initable, PreInitable, ServerStartedListener, PlayerMessageListener {
    public static final Logger logger = Logger.getLogger("SteamTracker");

    @Override
    public void init() {
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();

            CtClass ctLoginHandler = classPool.getCtClass("com.wurmonline.server.LoginHandler");

            CtMethod mHandleLogin = ctLoginHandler.getMethod("handleLogin", "(Ljava/lang/String;Ljava/lang/String;ZZZZLjava/lang/String;Ljava/lang/String;)V");
            mHandleLogin.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("initialisePlayer")) {
                        m.replace("$proceed($$); net.bdew.wurm.steamtracker.SteamTrackerHook.track($0, steamIDAsString);");
                        logger.info("Installed normal track hook in handleLogin at line " + m.getLineNumber());
                    } else if (m.getMethodName().equals("sendReconnect") && m.getClassName().equals("com.wurmonline.server.creatures.Communicator")) {
                        m.replace("$proceed($$); net.bdew.wurm.steamtracker.SteamTrackerHook.track($0.player, steamIDAsString);");
                        logger.info("Installed reconnect track hook in handleLogin at line " + m.getLineNumber());
                    } else if (m.getMethodName().equals("setWurmId")) {
                        m.replace("$proceed($$); net.bdew.wurm.steamtracker.SteamTrackerHook.track($0, steamIDAsString);");
                        logger.info("Installed new player track hook in handleLogin at line " + m.getLineNumber());
                    }
                }
            });
            mHandleLogin.insertBefore(
                    "if (net.bdew.wurm.steamtracker.SteamTrackerHook.isBanned(name, steamIDAsString)) {" +
                            "com.wurmonline.server.Server.getInstance().steamHandler.removeIsPlayerAuthenticated(steamIDAsString);" +
                            "com.wurmonline.server.Server.getInstance().steamHandler.EndAuthSession(steamIDAsString);" +
                            "this.sendLoginAnswer(false, \"You are banned.\", 0.0f, 0.0f, 0.0f, 0.0f, 0, \"model.player.broken\", (byte)0, 0);" +
                            "return;" +
                            "};"
            );

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preInit() {
    }

    @Override
    public void onServerStarted() {
        try {
            SteamTrackerDB.init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        return SteamTrackerCommands.handleMessage(communicator, message, title);
    }
}
