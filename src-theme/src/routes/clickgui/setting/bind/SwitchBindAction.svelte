<script lang="ts">
    import type {BindAction} from "../../../../integration/types";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {fly} from "svelte/transition";
    import {cubicOut} from 'svelte/easing';

    export let choices: BindAction[];
    export let chosen: typeof choices[number];
    export let onchange: () => any;

    let direction = 1;

    /**
     * Switch item among {@link choices}.
     */
    function switchAction() {
        const currentIndex = choices.indexOf(chosen);
        if (currentIndex === -1) {
            throw new Error("Unexpected action: " + chosen);
        }

        const nextIndex = (currentIndex + direction) % choices.length;
        chosen = choices[nextIndex];

        onchange();
    }
</script>

<button on:click|stopPropagation={switchAction}>
    <span class="chosen-holder">
        {#key chosen}
            <span
                    class="chosen"
                    in:fly={{ x: direction * 20, duration: 200, delay: 200, easing: cubicOut }}
                    out:fly={{ x: -direction * 20, duration: 200, easing: cubicOut }}
            >{chosen}</span>
        {/key}
    </span>
    <ExpandArrow
            expanded={false}
            expandable={false}
            compact={true}
            dimmed={true}
    />
</button>

<style lang="scss">
  @use "../../../../colors" as *;

  .chosen-holder {
    display: grid;

    .chosen {
      font-weight: 500;
      color: $clickgui-text-color;
      font-size: 12px;
      text-overflow: ellipsis;
      white-space: nowrap;
      grid-column: 1/1;
      grid-row: 1/1;
    }
  }

  button {
    all: unset;
    background: none;
    padding: 0;
    cursor: pointer;
    display: flex;
    gap: 3px;
    align-items: center;
    position: relative;
    border: none;
  }
</style>
