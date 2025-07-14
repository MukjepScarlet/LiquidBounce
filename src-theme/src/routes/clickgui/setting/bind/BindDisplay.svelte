<script lang="ts">
    import type {BindModifier} from "../../../../integration/types";

    export let modifiers: BindModifier[] | Set<BindModifier>;
    export let boundKey: string | undefined;

    $: parts = [...modifiers, boundKey]
        .filter(Boolean)
        .flatMap((item, index, array) =>
            index < array.length - 1
                ? [item, "+"]
                : [item]
        );
</script>

<span class="wrapper">
    {#if boundKey}
        {#each parts as part}
            <span class:muted={part === "+"}>{part}</span>
        {/each}
    {:else}
        <span class="muted">None</span>
    {/if}
</span>

<style lang="scss">
  @use "../../../../colors" as *;


  .wrapper {
    column-gap: 3px;
    display: flex;
  }

  .muted {
    color: $clickgui-text-dimmed-color
  }
</style>
