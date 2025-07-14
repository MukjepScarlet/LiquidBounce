<script lang="ts">
    import {createEventDispatcher, onDestroy} from "svelte";
    import type {BindModifier, BindSetting, ModuleSetting, Screen} from "../../../../integration/types";
    import {waitMatches} from "../../../../integration/ws";
    import {getPrintableKeyName} from "../../../../integration/rest";
    import type {KeyboardKeyEvent, MouseButtonEvent} from "../../../../integration/events";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import BindDisplay from "./BindDisplay.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as BindSetting;

    const UNKNOWN_KEY = "key.keyboard.unknown";

    const dispatch = createEventDispatcher();

    let isHovered = false;
    let binding = false;
    let printableKeyName: string | undefined;

    $: {
        if (cSetting.value.boundKey !== UNKNOWN_KEY) {
            getPrintableKeyName(cSetting.value.boundKey)
                .then(printableKey => {
                    printableKeyName = printableKey.localized;
                });
        } else {
            printableKeyName = undefined;
        }
    }

    const isClickGuiScreen = (screen: Screen | undefined) =>
        !(screen === undefined || !screen.class.startsWith("net.ccbluex.liquidbounce") || screen.title !== "ClickGUI" && screen.title !== "VS-CLICKGUI");

    /**
     * Gets the next possible event which can be used as a bind.
     */
    const nextBindEvent = () => Promise.any([
        waitMatches("mouseButton", (e: MouseButtonEvent) =>
            isClickGuiScreen(e.screen) && !(e.button === 0 /* LMB */ && isHovered)
        ),
        waitMatches("keyboardKey", (e: KeyboardKeyEvent) =>
            isClickGuiScreen(e.screen)
        ),
    ]);

    let addedModifiers = new Set<BindModifier>();

    /**
     * Tries to handle the event. If it's consumed
     * @return undefined if it's not a modifier, or else its key name and parsed modifier
     */
    const handleBindEventIfNotModifier = (event: MouseButtonEvent | KeyboardKeyEvent) => {
        if (Object.hasOwn(event, 'keyCode')) {
            const e = event as KeyboardKeyEvent;
            if (e.keyCode === 256 /* GLFW_KEY_ESCAPE */) {
                handleActionChange(UNKNOWN_KEY);
                return undefined;
            }

            const modifierOrUndef = KEY_TOKEN_TO_MODIFIERS[e.keyCode];

            if (!modifierOrUndef) {
                handleActionChange(e.key);
                return undefined;
            }

            return { key: e.key, keyCode: e.keyCode, modifier: modifierOrUndef };
        } else if (Object.hasOwn(event, 'button')) {
            const e = event as MouseButtonEvent;
            handleActionChange(e.key);
            return undefined;
        } else {
            throw new Error("Unexcepted event: " + JSON.stringify(event));
        }
    }

    let timeout: ReturnType<typeof setTimeout> | undefined = undefined;
    onDestroy(() => {
        if (timeout !== undefined) {
            clearTimeout(timeout);
        }
    });

    async function toggleBinding() {
        // Binding progress -> cancel it
        if (binding) {
            handleActionChange(UNKNOWN_KEY);
            return;
        }

        binding = true;

        let event = await nextBindEvent();
        // Promise doesn't support cancellation, so we need manual check
        if (!binding) return;
        let result = handleBindEventIfNotModifier(event);
        while (result) {
            if (timeout !== undefined) {
                clearTimeout(timeout);
            }
            const { key, modifier } = result;

            addedModifiers.add(modifier);
            addedModifiers = addedModifiers; // Trigger reactive update

            timeout = setTimeout(() => {
                if (binding) {
                    handleActionChange(key);
                }
                timeout = undefined;
            }, 1000);

            event = await nextBindEvent();
            if (!binding) return;
            result = handleBindEventIfNotModifier(event);
        }
    }

    function handleActionChange(newBoundKey: string) {
        addedModifiers.delete(KEY_CODE_TO_MODIFIERS[newBoundKey]); // We don't want Shift+RIGHT_SHIFT
        cSetting.value.boundKey = newBoundKey;
        cSetting.value.modifiers = Array.from(addedModifiers);
        addedModifiers.clear();
        binding = false;
        setting = {...cSetting};
        dispatch("change");
    }

    /**
     * Switch action among {@link BindAction}.
     */
    function switchAction() {
        if (cSetting.value.action === "Toggle") {
            cSetting.value.action = "Hold";
        } else if (cSetting.value.action === "Hold") {
            cSetting.value.action = "Toggle";
        } else {
            throw new Error("Unexcepted action: " + cSetting.value.action);
        }

        setting = {...cSetting};
        dispatch("change");
    }

    /**
     * https://www.glfw.org/docs/3.3/group__keys.html
     */
    const KEY_TOKEN_TO_MODIFIERS: Record<number, BindModifier> = {
        340: "Shift", 344: "Shift",
        341: "Control", 345: "Control",
        342: "Alt", 346: "Alt",
        343: "Super", 347: "Super",
    } as const;

    /**
     * From Minecraft InputUtil.Type
     */
    const KEY_CODE_TO_MODIFIERS: Record<string, BindModifier> = {
        "key.keyboard.left.shift": "Shift", "key.keyboard.right.shift": "Shift",
        "key.keyboard.left.control": "Control", "key.keyboard.right.control": "Control",
        "key.keyboard.left.alt": "Alt", "key.keyboard.right.alt": "Alt",
        "key.keyboard.left.win": "Super", "key.keyboard.right.win": "Super",
    } as const;
</script>

<div class="setting" class:has-value={cSetting.value.boundKey !== UNKNOWN_KEY}>
    <button
            class="change-bind"
            on:click={toggleBinding}
            on:mouseenter={() => isHovered = true}
            on:mouseleave={() => isHovered = false}
    >
        {#if !binding}
            {#if cSetting.value.modifiers.length < 3}
                <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}:</div>
            {/if}

            <BindDisplay
                    bind:modifiers={cSetting.value.modifiers}
                    bind:boundKey={printableKeyName}
            />
        {:else if addedModifiers.size}
            <BindDisplay
                    bind:modifiers={addedModifiers}
                    boundKey="..."
            />
        {:else}
            <span>Press any key...</span>
        {/if}
    </button>

    {#if cSetting.value.boundKey !== UNKNOWN_KEY}
        <button class="action" on:click={switchAction}>
            <span>{cSetting.value.action}</span>
        </button>
    {/if}
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .setting {
    padding: 7px 0;
    display: grid;
    grid-template-columns: 1fr;
    column-gap: 5px;

    &.has-value {
      grid-template-columns: 1fr max-content;
    }
  }

  .change-bind {
    background-color: transparent;
    border: solid 2px $accent-color;
    border-radius: 3px;
    cursor: pointer;
    padding: 4px;
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    font-family: "Inter", sans-serif;
    width: 100%;
    display: flex;
    justify-content: center;
    column-gap: 5px;

    .name {
      display: inline-flex;
      align-items: center;
      font-weight: 500;
    }

    .none {
      color: $clickgui-text-dimmed-color;
    }
  }

  .action {
    all: unset;
    background-color: $accent-color;
    padding: 6px 10px;
    cursor: pointer;
    display: flex;
    align-items: center;
    position: relative;
    border-radius: 3px;

    span {
      font-weight: 500;
      color: $clickgui-text-color;
      font-size: 12px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
</style>
