<script lang="ts">
    import {createEventDispatcher, onDestroy} from "svelte";
    import type {BindModifier, BindSetting, ModuleSetting, Screen} from "../../../integration/types";
    import {listen, waitMatches} from "../../../integration/ws";
    import {getPrintableKeyName} from "../../../integration/rest";
    import type {KeyboardKeyEvent, MouseButtonEvent} from "../../../integration/events";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import Dropdown from "./common/Dropdown.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as BindSetting;

    const UNKNOWN_KEY = "key.keyboard.unknown";

    const dispatch = createEventDispatcher();

    let isHovered = false;
    let binding = false;
    let printableKeyName = "";

    $: {
        if (cSetting.value.boundKey !== UNKNOWN_KEY) {
            getPrintableKeyName(cSetting.value.boundKey)
                .then(printableKey => {
                    printableKeyName = printableKey.localized;
                });
        }
    }

    const isClickGuiScreen = (screen: Screen | undefined) =>
        !(screen === undefined || !screen.class.startsWith("net.ccbluex.liquidbounce") || screen.title !== "ClickGUI" && screen.title !== "VS-CLICKGUI")

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

            const modifierOrUndef = KEY_CODE_TO_MODIFIERS[e.keyCode];

            if (!modifierOrUndef) {
                handleActionChange(e.key);
                return undefined;
            }

            return { key: e.key, modifier: modifierOrUndef };
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
        cSetting.value.boundKey = newBoundKey;
        cSetting.value.modifiers = Array.from(addedModifiers);
        addedModifiers.clear();
        binding = false;
        setting = {...cSetting};
        dispatch("change");
    }

    /**
     * Switch action among {@link BindAction}.
     *
     * TODO: rewrite this
     */
    function switchAction() {
        setting = {...cSetting};
        dispatch("change");
    }

    /**
     * @deprecated
     * @param modifier
     */
    function toggleModifier(modifier: BindModifier) {
        if (cSetting.value.modifiers.includes(modifier)) {
            cSetting.value.modifiers = cSetting.value.modifiers.filter(it => it !== modifier);
        } else {
            cSetting.value.modifiers = [...cSetting.value.modifiers, modifier];
        }
    }

    /**
     * https://www.glfw.org/docs/3.3/group__keys.html
     */
    const KEY_CODE_TO_MODIFIERS: Record<number, BindModifier> = {
        340: "Shift", 344: "Shift",
        341: "Control", 345: "Control",
        342: "Alt", 346: "Alt",
        343: "Super", 347: "Super",
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
            <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}:</div>

            {#if cSetting.value.boundKey === UNKNOWN_KEY}
                <span class="none">None</span>
            {:else}
                <span>{printableKeyName}</span>
            {/if}
        {:else if addedModifiers.size}
            <span>{Array.from(addedModifiers).join(" + ")} + ...</span>
        {:else}
            <span>Press any key...</span>
        {/if}
    </button>

    {#if cSetting.value.boundKey !== UNKNOWN_KEY}
        <!-- TODO: replace with click to switch... -->
        <Dropdown name={null} options={["Toggle", "Hold"]} bind:value={cSetting.value.action}
                  on:change={switchAction} style="grid-area: action"/>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
    display: grid;
    grid-template-columns: 1fr;
    gap: 5px;

    &.has-value {
      grid-template-columns: 1fr max-content;
      grid-template-rows: auto auto;
      grid-template-areas: "main action";
    }

    &:not(.has-value) {
      grid-template-columns: 1fr;
      grid-template-areas: "main";
    }
  }

  .change-bind {
    grid-area: main;
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
      font-weight: 500;
    }

    .none {
      color: $clickgui-text-dimmed-color;
    }
  }
</style>
