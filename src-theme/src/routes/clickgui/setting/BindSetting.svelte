<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BindModifier, BindSetting, ModuleSetting, Screen} from "../../../integration/types";
    import {listen, waitNext} from "../../../integration/ws";
    import {getPrintableKeyName} from "../../../integration/rest";
    import type {KeyboardKeyEvent, MouseButtonEvent} from "../../../integration/events";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import Dropdown from "./common/Dropdown.svelte";
    import MultiChoiceItem from "./common/MultiChoiceItem.svelte";

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

    async function toggleBinding() {
        // Binding progress -> cancel it
        if (binding) {
            handleActionChange(UNKNOWN_KEY);
            return;
        }

        binding = true;

        const firstIncomingEvent = await Promise.any([
            waitNext("mouseButton", (e: MouseButtonEvent) =>
                isClickGuiScreen(e.screen) && !(e.button === 0 /* LMB */ && isHovered)
            ),
            waitNext("keyboardKey", (e: KeyboardKeyEvent) =>
                isClickGuiScreen(e.screen)
            ),
        ]);

        // Promise doesn't support cancellation, so we need manual check
        if (!binding) return;

        if (Object.hasOwn(firstIncomingEvent, 'keyCode')) {
            const e = firstIncomingEvent as KeyboardKeyEvent;
            if (e.keyCode === 256 /* GLFW_KEY_ESCAPE */) {
                handleActionChange(UNKNOWN_KEY);
                return;
            }

            handleActionChange(e.key);
            return;
        } else if (Object.hasOwn(firstIncomingEvent, 'button')) {
            const e = firstIncomingEvent as MouseButtonEvent;
            handleActionChange(e.key);
            return;
        } else {
            throw new Error("Unexcepted event: " + JSON.stringify(firstIncomingEvent));
        }
    }

    function handleActionChange(newBoundKey: string) {
        cSetting.value.boundKey = newBoundKey;
        binding = false;
        setting = {...cSetting};
        dispatch("change");
    }

    function toggleModifier(modifier: BindModifier) {
        if (cSetting.value.modifiers.includes(modifier)) {
            cSetting.value.modifiers = cSetting.value.modifiers.filter(it => it !== modifier);
        } else {
            cSetting.value.modifiers = [...cSetting.value.modifiers, modifier];
        }
    }

    const ALL_MODIFIERS = ["Shift", "Control", "Alt", "Super"] as const;

    /**
     *
     */
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
        {:else}
            <span>Press any key...</span>
        {/if}
    </button>

    {#if cSetting.value.boundKey !== UNKNOWN_KEY}
        <Dropdown name={null} options={["Toggle", "Hold"]} bind:value={cSetting.value.action}
                  on:change={handleActionChange} style="grid-area: action"/>
        <div class="modifiers">
            {#each ALL_MODIFIERS as modifier (modifier)}
                <MultiChoiceItem
                    active={cSetting.value.modifiers.includes(modifier)}
                    error={false}
                    onclick={() => toggleModifier(modifier)}
                    content={$spaceSeperatedNames ? convertToSpacedString(modifier) : modifier}
                />
            {/each}
        </div>
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
      grid-template-areas:
          "main action"
          "modifiers modifiers";
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

  .modifiers {
    display: flex;
    flex-direction: row;
    font-size: 12px;
    gap: 7px;
    justify-content: space-between;
    flex-wrap: wrap;
    grid-area: modifiers;
  }
</style>
