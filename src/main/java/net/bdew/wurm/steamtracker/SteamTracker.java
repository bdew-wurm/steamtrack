package net.bdew.wurm.steamtracker;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import java.sql.SQLException;
import java.util.logging.Logger;

public class SteamTracker implements WurmMod, Initable, PreInitable, ServerStartedListener {
    public static final Logger logger = Logger.getLogger("SteamTracker");

    @Override
    public void init() {
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();

            CtClass ctLoginHandler = classPool.getCtClass("com.wurmonline.server.LoginHandler");
            ctLoginHandler.getMethod("handleLogin", "(Ljava/lang/String;Ljava/lang/String;ZZZZLjava/lang/String;Ljava/lang/String;)V").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("initialisePlayer")) {
                        m.replace("$proceed($$); net.bdew.wurm.steamtracker.SteamTrackerHook.track($0, $0.SteamId);");
                        logger.info("Installed track hook in handleLogin at line " + m.getLineNumber());
                    }
                }
            });

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
}
