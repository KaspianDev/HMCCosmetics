package io.github.fisher2911.hmccosmetics.config;

import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.GuiItem;
import io.github.fisher2911.hmccosmetics.HMCCosmetics;
import io.github.fisher2911.hmccosmetics.gui.ArmorItem;
import io.github.fisher2911.hmccosmetics.hook.HookManager;
import io.github.fisher2911.hmccosmetics.util.Keys;
import io.github.fisher2911.hmccosmetics.util.StringUtils;
import io.github.fisher2911.hmccosmetics.util.Utils;
import io.github.fisher2911.hmccosmetics.util.builder.ColorBuilder;
import io.github.fisher2911.hmccosmetics.util.builder.ItemBuilder;
import io.github.fisher2911.hmccosmetics.util.builder.SkullBuilder;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

public class ItemSerializer implements TypeSerializer<GuiItem> {

    public static final ItemSerializer INSTANCE = new ItemSerializer();
    private static final HMCCosmetics plugin;
    private static final String MATERIAL = "material";
    private static final String AMOUNT = "amount";
    private static final String NAME = "name";
    private static final String UNBREAKABLE = "unbreakable";
    private static final String GLOWING = "glowing";
    private static final String LORE = "lore";
    private static final String LOCKED_LORE = "locked-lore";
    private static final String MODEL_DATA = "model-data";
    private static final String ENCHANTS = "enchants";
    private static final String ITEM_FLAGS = "item-flags";
    private static final String TEXTURE = "texture";
    private static final String OWNER = "owner";
    private static final String COLOR = "color";
    private static final String RED = "red";
    private static final String GREEN = "green";
    private static final String BLUE = "blue";
    private static final String PERMISSION = "permission";
    private static final String TYPE = "type";
    private static final String ACTION = "action";
    private static final String ID = "id";
    private static final String DYEABLE = "dyeable";

    static {
        plugin = HMCCosmetics.getPlugin(HMCCosmetics.class);
    }

    private ItemSerializer() {
    }

    private ConfigurationNode nonVirtualNode(final ConfigurationNode source, final Object... path)
            throws SerializationException {
        if (!source.hasChild(path)) {
            throw new SerializationException(
                    "Required field " + Arrays.toString(path) + " was not present in node");
        }

        return source.node(path);
    }

    @Override
    public GuiItem deserialize(final Type type, final ConfigurationNode source)
            throws SerializationException {
        final ConfigurationNode materialNode = this.nonVirtualNode(source, MATERIAL);
        final ConfigurationNode amountNode = source.node(AMOUNT);
        final ConfigurationNode nameNode = source.node(NAME);
        final ConfigurationNode unbreakableNode = source.node(UNBREAKABLE);
        final ConfigurationNode glowingNode = source.node(GLOWING);
        final ConfigurationNode loreNode = source.node(LORE);
        final ConfigurationNode lockedLoreNode = source.node(LOCKED_LORE);
        final ConfigurationNode modelDataNode = source.node(MODEL_DATA);
        final ConfigurationNode enchantsNode = source.node(ENCHANTS);
        final ConfigurationNode itemFlagsNode = source.node(ITEM_FLAGS);
        final ConfigurationNode textureNode = source.node(TEXTURE);
        final ConfigurationNode ownerNode = source.node(OWNER);
        final ConfigurationNode colorNode = source.node(COLOR);
        final ConfigurationNode redNode = colorNode.node(RED);
        final ConfigurationNode greenNode = colorNode.node(GREEN);
        final ConfigurationNode blueNode = colorNode.node(BLUE);
        final ConfigurationNode permissionNode = source.node(PERMISSION);
        final ConfigurationNode typeNode = source.node(TYPE);
        final ConfigurationNode actionNode = source.node(ACTION);
        final ConfigurationNode idNode = source.node(ID);
        final ConfigurationNode dyeableNode = source.node(DYEABLE);

        final String materialString = Utils.replaceIfNull(materialNode.getString(), "");
        final int amount = amountNode.getInt();

        ItemStack itemStack;

        try {
            itemStack = new ItemStack(Material.valueOf(materialString), amount);
        } catch (final IllegalArgumentException exception) {
            itemStack = HookManager.getInstance().getItemHooks().getItemStack(materialString);
            if (itemStack == null) {
                itemStack = new ItemStack(Material.AIR);
            }
        }

        final String name = StringUtils.parseStringToString(nameNode.getString());

        final boolean unbreakable = unbreakableNode.getBoolean();
        final boolean glowing = glowingNode.getBoolean();

        final List<String> lore = Utils.replaceIfNull(loreNode.getList(String.class),
                        new ArrayList<String>()).
                stream().map(StringUtils::parseStringToString).collect(Collectors.toList());

        final List<String> lockedLore = Utils.replaceIfNull(lockedLoreNode.getList(String.class),
                        new ArrayList<String>()).
                stream().map(StringUtils::parseStringToString).collect(Collectors.toList());

        final int modelData = modelDataNode.getInt();

        final Set<ItemFlag> itemFlags = Utils.replaceIfNull(itemFlagsNode.getList(String.class),
                        new ArrayList<String>()).
                stream().map(flag -> {
                    try {
                        return ItemFlag.valueOf(flag.toUpperCase());
                    } catch (final Exception ignored) {
                        return null;
                    }
                }).collect(Collectors.toSet());
        final String texture = textureNode.getString();
        final String owner = ownerNode.getString();

        final boolean dyeable = dyeableNode.getBoolean();

        final Color color;

        if (colorNode.virtual()) {
            color = null;
        } else {
            color = Color.fromRGB(redNode.getInt(), greenNode.getInt(), blueNode.getInt());
        }

        final Map<Enchantment, Integer> enchantments =
                Utils.replaceIfNull(enchantsNode.getList(String.class),
                                new ArrayList<String>()).
                        stream().
                        collect(Collectors.toMap(enchantmentString -> {

                            if (!enchantmentString.contains(":")) {
                                return null;
                            }

                            final NamespacedKey namespacedKey = NamespacedKey.minecraft(
                                    enchantmentString.
                                            split(":")[0].
                                            toLowerCase());
                            return Registry.ENCHANTMENT.get(namespacedKey);

                        }, enchantmentString -> {
                            if (!enchantmentString.contains(":")) {
                                return 0;
                            }
                            try {
                                return Integer.parseInt(enchantmentString.split(":")[1]);
                            } catch (final NumberFormatException exception) {
                                return 0;
                            }
                        }));

        final ItemBuilder itemBuilder;

        if (itemStack.getType() == Material.PLAYER_HEAD) {
            itemBuilder = SkullBuilder.create();

            if (texture != null) {
                ((SkullBuilder) itemBuilder).texture(texture);
            } else if (owner != null) {
                final OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
                ((SkullBuilder) itemBuilder).owner(player);
            }
        } else if (ColorBuilder.canBeColored(itemStack)) {
            itemBuilder = ColorBuilder.from(itemStack);
            if (color != null) {
                ((ColorBuilder) itemBuilder).color(color);
            }
        } else {
            itemBuilder = ItemBuilder.from(itemStack);
        }

        if (itemStack.getItemMeta() != null && !itemStack.getItemMeta().hasCustomModelData()) {
            itemBuilder.modelData(modelData);
        }

        itemStack = itemBuilder.
                amount(amount).
                name(name).
                unbreakable(unbreakable).
                glow(glowing).
                lore(lore).
                enchants(enchantments, true).
                itemFlags(itemFlags).
                build();

        final GuiAction<InventoryClickEvent> action = ActionSerializer.INSTANCE.deserialize(GuiAction.class, actionNode);

        Keys.setKey(itemStack);

        try {
            final ArmorItem.Type cosmeticType = ArmorItem.Type.valueOf(
                    Utils.replaceIfNull(
                            typeNode.getString(), ""
                    ).toUpperCase(Locale.ROOT)
            );

            final String permission = permissionNode.getString();

            return new ArmorItem(
                    itemStack,
                    action,
                    Utils.replaceIfNull(idNode.getString(), ""),
                    lockedLore,
                    permission,
                    cosmeticType,
                    dyeable,
                    -1
            );

        } catch (final IllegalArgumentException exception) {
            final GuiItem guiItem = dev.triumphteam.gui.builder.item.ItemBuilder.from(itemStack).asGuiItem();
            guiItem.setAction(action);
            return guiItem;
        }
    }

    @Override
    public void serialize(final Type type, @Nullable final GuiItem obj, final ConfigurationNode node) throws SerializationException {

    }

}
