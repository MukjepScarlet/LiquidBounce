<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BindSetting, ModuleSetting} from "../../../integration/types";
    import {listen} from "../../../integration/ws";
    import {getPrintableKeyName} from "../../../integration/rest";
    import type {KeyboardKeyEvent, MouseButtonEvent} from "../../../integration/events";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import Dropdown from "./common/Dropdown.svelte";
    import MultiDropdown from "./common/MultiDropdown.svelte";

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
                    // let keyName = printableKey.localized;
                    // if (cSetting.value.modifiers?.length) {
                    //     for (const modifier of cSetting.value.modifiers) {
                    //         keyName += '+' + modifier;
                    //     }
                    // }
                    //
                    // printableKeyName = keyName;
                    printableKeyName = printableKey.localized;
                });
        }
    }

    listen("keyboardKey", async (e: KeyboardKeyEvent) => {
        if (e.screen === undefined || !e.screen.class.startsWith("net.ccbluex.liquidbounce") ||
            !(e.screen.title === "ClickGUI" || e.screen.title === "VS-CLICKGUI")) {
            return;
        }

        if (!binding) {
            return;
        }

        binding = false;

        if (e.keyCode !== 256) {
            cSetting.value.boundKey = e.key;
        } else {
            cSetting.value.boundKey = UNKNOWN_KEY;
        }

        setting = {...cSetting};

        dispatch("change");
    });

    listen("mouseButton", async (e: MouseButtonEvent) => {
        if (e.screen === undefined || !e.screen.class.startsWith("net.ccbluex.liquidbounce") ||
            !(e.screen.title === "ClickGUI" || e.screen.title === "VS-CLICKGUI")) {
            return;
        }

        if (!binding || (e.button === 0 && isHovered)) {
            return;
        }

        binding = false;

        cSetting.value.boundKey = e.key;

        setting = {...cSetting};

        dispatch("change");
    })

    async function toggleBinding() {
        if (binding) {
            cSetting.value.boundKey = UNKNOWN_KEY;
        }

        binding = !binding;

        setting = {...cSetting};

        dispatch("change");
    }

    function handleActionChange() {
        setting = {...cSetting};
        dispatch("change");
    }
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
            <span>Press any key</span>
        {/if}
    </button>

    {#if cSetting.value.boundKey !== UNKNOWN_KEY}
        <Dropdown name={null} options={["Toggle", "Hold"]} bind:value={cSetting.value.action}
                  on:change={handleActionChange} style="grid-area: action"/>
        <MultiDropdown
                name="Modifiers"
                options={["Shift", "Control", "Alt", "Super"]}
                bind:values={cSetting.value.modifiers}
                on:change={handleActionChange}
                style="grid-area: modifiers"
                separator=" + "
        />
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
</style>
