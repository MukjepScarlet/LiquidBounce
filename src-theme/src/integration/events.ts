import type {
    Component,
    ConfigurableSetting,
    ItemStack,
    PlayerData,
    Proxy,
    Screen,
    Server,
    TextComponent,
} from "./types";

export interface Event {
}

export interface ClickGuiValueChangeEvent extends Event {
    configurable: ConfigurableSetting;
}

export interface ModuleToggleEvent extends Event {
    moduleName: string;
    hidden: boolean;
    enabled: boolean;
}

export interface KeyboardKeyEvent extends Event {
    keyCode: number;
    scanCode: number;
    action: number;
    mods: number;
    key: string;
    screen: Screen | undefined;
}

export interface MouseButtonEvent extends Event {
    key: string;
    button: number;
    action: number;
    mods: number;
    screen: Screen | undefined;
}

export interface ScaleFactorChangeEvent extends Event {
    scaleFactor: number;
}

export interface ComponentsUpdateEvent extends Event {
    components: Component[];
}

export interface ClientPlayerDataEvent extends Event {
    playerData: PlayerData;
}

export interface OverlayMessageEvent extends Event {
    text: TextComponent | string;
    tinted: boolean;
}

export interface NotificationEvent extends Event {
    title: string;
    message: string;
    severity: "INFO" | "SUCCESS" | "ERROR" | "ENABLED" | "DISABLED";
}

export interface KeyEvent extends Event {
    key: string;
    action: number;
    mods: number;
}

export interface TargetChangeEvent extends Event {
    target: PlayerData | null;
}

export interface BlockCountChangeEvent extends Event {
    count?: number;
}

export interface AccountManagerAdditionEvent extends Event {
    username: string | null;
    error: string | null;
}

export interface AccountManagerRemovalEvent extends Event {
    username: string | null;
}

export interface AccountManagerMessageEvent extends Event {
    message: string;
}

export interface AccountManagerLoginEvent extends Event {
    username: string | null;
    error: string | null;
}

export interface ServerPingedEvent extends Event {
    server: Server;
}

export interface PlayerInventoryEvent extends Event {
    inventory: PlayerInventory;
}

export interface PlayerInventory {
    armor: ItemStack[];
    main: ItemStack[];
    crafting: ItemStack[];
}

export interface ProxyAdditionResultEvent extends Event {
    proxy: Proxy | null;
    error: string | null;
}

export interface ProxyEditResultEvent extends Event {
    proxy: Proxy | null;
    error: string | null;
}

export interface ProxyCheckResultEvent extends Event {
    proxy: Proxy;
    error: string | null;
}

export interface SpaceSeperatedNamesChangeEvent extends Event {
    value: boolean;
}

export interface ClickGuiScaleChangeEvent extends Event {
    value: number;
}

export interface BrowserUrlChangeEvent extends Event {
    url: string;
}
