package net.myitian.blockyfile.integration.modmenu;


import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.myitian.blockyfile.BlockyFile;
import net.myitian.blockyfile.integration.clothconfig.ConfigScreen;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (BlockyFile.CLOTH_CONFIG_EXISTED) {
            return ConfigScreen::buildConfigScreen;
        } else {
            return parent -> null;
        }
    }
}