<template>
	<router-link
		class="h-64 w-52 rounded-xl focus-visible:outline-offset-0 focus-visible:outline-blue-500"
		:to="'/listings/' + cardInfo.id"
	>
		<div
			class="group flex flex-col overflow-hidden rounded-xl border bg-white shadow-sm transition hover:shadow-lg"
			href="#"
		>
			<div
				class="relative overflow-hidden rounded-t-xl pt-[80%] sm:pt-[80%] lg:pt-[80%]"
			>
				<img
					class="absolute left-0 top-0 h-full w-full rounded-t-xl object-cover transition-transform duration-500 ease-in-out group-hover:scale-110"
					:src="imageSrc"
					alt="Image Description"
				/>
			</div>
			<div class="flex flex-col gap-2 p-4 md:p-5">
				<span class="text-lg font-bold text-gray-800">{{ price }}</span>
				<p class="truncate text-xs text-gray-800">
					{{ cardInfo.name }}
				</p>
			</div>
		</div>
	</router-link>
</template>
<script setup>
	import { ref, onBeforeMount } from "vue";
	import { getListingThumbanil } from "@/utils/axios";
	import { formatMoney } from "@/utils/strings";

	const props = defineProps({
		cardInfo: {
			id: Number,
			price: String,
			name: String,
			media: String,
		},
	});

	const imageSrc = ref("#");

	onBeforeMount(async () => {
		const response = await getListingThumbanil(props.cardInfo.id);
		if (response.error) {
			// error
		} else {
			imageSrc.value = response.data;
		}
	});

	const price = ref(formatMoney(props.cardInfo.price));
</script>
