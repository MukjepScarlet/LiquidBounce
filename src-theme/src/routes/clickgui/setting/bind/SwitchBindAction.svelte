<script lang="ts">
    import type {BindAction} from "../../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import ExpandArrow from "../common/ExpandArrow.svelte";

    export let action: BindAction;

    const dispatch = createEventDispatcher();

    const bindActions: BindAction[] = ["Toggle", "Hold"];

    /**
     * Switch action among {@link BindAction}.
     */
    function switchAction() {
        const currentIndex = bindActions.indexOf(action);
        if (currentIndex === -1) {
            throw new Error("Unexpected action: " + action);
        }

        const nextIndex = (currentIndex + 1) % bindActions.length;
        action = bindActions[nextIndex];

        dispatch("change");
    }
</script>

<button on:click|stopPropagation={switchAction}>
    <span class="action">{action}</span>
    <ExpandArrow
            expanded={false}
            expandable={false}
            compact={true}
            dimmed={true}
    />
</button>

<style lang="scss">
  @use "../../../../colors" as *;

  .action {
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
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
