<script lang="ts">
    import type {BindAction} from "../../../../integration/types";
    import {createEventDispatcher} from "svelte";

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

<button class="action" on:click={switchAction}>
    <span>{action}</span>
</button>

<style lang="scss">
  @use "../../../../colors" as *;

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
