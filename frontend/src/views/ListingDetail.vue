<script setup>
	import { computed, onBeforeMount, reactive, ref } from "vue";
	import { onBeforeRouteUpdate, useRoute, useRouter } from "vue-router";
	import { useUserStore } from "@/stores/user";
	import { useQuestionSection } from "@/stores/questionSection";
	import StarsInput from "@/components/Inputs/StarsInput.vue";
	import QuantitySelect from "@/components/Inputs/QuantitySelect.vue";
	import ImageSelector from "@/components/Images/ImageSelector.vue";
	import QuestionsSection from "@/components/QuestionsSection/BaseLayout.vue";
	import GenericModal from "@/components/GenericModal.vue";
	import { getListingById, getListingImages, postSale } from "@/utils/axios";
	import { getArticleOfSentence, formatMoney } from "@/utils/strings";

	const route = useRoute();
	const router = useRouter();
	const listingId = ref(route.params.id);
	const listing = ref(null);
	const desiredQuantity = ref(1);
	const isUserSure = ref(false);
	const modal = reactive({ title: "", description: null, open: false });
	const user = useUserStore();
	const questionSection = useQuestionSection();
	const isUserTheSeller = computed(
		() => listing?.value?.seller?.id === user.id
	);

	questionSection.listingId = Number(listingId.value);
	questionSection.isUserTheSeller = isUserTheSeller;

	onBeforeMount(async () => {
		if (Number.isNaN(Number(listingId.value))) {
			router.push("/404");
			return;
		}
		getListingData();
		getProductImages();
	});

	onBeforeRouteUpdate((to, from, next) => {
		listingId.value = to.params.id;
		getListingData();
		getProductImages();
		next();
	});

	const getListingData = () => {
		getListingById(listingId.value).then(response => {
			const { data } = response;
			const isValid = validateListingFetch(data);
			if (!isValid) return;

			if (!listing.value) listing.value = data;
			else listing.value = { ...listing.value, ...data };
		});
	};

	const getProductImages = () => {
		getListingImages(listingId.value).then(response => {
			const { data } = response;
			const isValid = validateListingFetch(data);
			if (!isValid || data.length === 0) listing.value.images = [];

			const thumbnails = [];

			const images = data.filter(img => {
				if (img.includes("thumb")) {
					thumbnails.push(img);
				}
				return !img.includes("thumb");
			});

			if (!listing.value) listing.value = { images };
			else listing.value.images = data;
		});
	};

	const validateListingFetch = data => {
		if (typeof data === "undefined") {
			modal.title = "Error 500";
			modal.description = "There was an error connecting with the server";
			modal.open = true;
			return false;
		}
		if (!data) {
			router.push("/404");
			return false;
		}
		return true;
	};

	const handleModalClose = () => {
		modal.title = "";
		modal.description = null;
		modal.open = false;
		isUserSure.value = false;
	};

	const handleQuantityUpdate = payload => {
		desiredQuantity.value = payload;
	};

	const makePurchase = async () => {
		const data = {
			buyer_id: user.id,
			listing_id: listing.value.id,
			quantity: desiredQuantity.value,
		};
		const result = await postSale(data);
		if (result.error) {
			modal.title = "Error";
			modal.description = "An error has ocurred, please try again later";
			modal.open = true;
			return;
		}
		isUserSure.value = true;
	};
</script>

<template>
	<generic-modal
		:title="modal.title"
		:description="modal.description"
		:open="modal.open"
		@modal-closed="handleModalClose"
	>
		<div v-if="!isUserSure">
			<h2>Are you sure?</h2>
			<div class="my-3 flex gap-4">
				<button
					class="endava h-10 w-20 px-5 py-2"
					@click="makePurchase"
				>
					Yes!
				</button>
				<button
					class="endava-gray h-10 w-20 px-5 py-2"
					@click="handleModalClose"
				>
					No
				</button>
			</div>
		</div>

		<div v-else>
			<h2>Your purchase is almost complete!😎</h2>
			<p class="my-2">
				To buy
				{{
					getArticleOfSentence(
						desiredQuantity,
						listing.name.charAt(0)
					)
				}}
				"{{ listing.name }}" from <b>{{ listing.seller.name }}</b
				>, use the contact information bellow
			</p>
			<h3>Seller contact Information:</h3>
			<ul>
				<li>{{ listing.seller.email }}</li>
			</ul>
		</div>
	</generic-modal>

	<main class="gah3-x-4 mx-14 my-12 grid grid-cols-1 lg:grid-cols-7">
		<image-selector
			v-if="listing && listing.images && listing.images.length > 0"
			:images="listing.images"
			styles="col-span-3 lg:col-span-4 pr-2"
		/>
		<section
			class="col-span-3 lg:col-span-3"
			v-if="listing && listing.name"
		>
			<h1>{{ listing.name }}</h1>
			<div class="text-lg font-semibold text-endava-600">
				{{ formatMoney(listing.price) }}
			</div>
			<div class="flex gap-2">
				<span>
					<StarsInput
						color="#DE411B"
						:increment="0.5"
						:default-rating="listing.condition"
						:disabled="true"
					/>
				</span>
				<span>(Condition)</span>
			</div>
			<div>Stock: {{ listing.stock }}</div>
			<p class="my-8 pr-16 text-gray-500">
				{{ listing.detail }}
			</p>

			<p class="my-4">
				Sold by:
				<span class="font-bold">{{
					isUserTheSeller ? "You" : listing.seller.name
				}}</span>
			</p>
			<form
				v-if="!isUserTheSeller && listing.stock > 0"
				@submit.prevent="modal.open = true"
				id="purchase-form"
			>
				<div class="my-4">
					<span class="mr-4">Quantity</span>
					<quantity-select
						:stock="listing.stock"
						@quantity-selected="handleQuantityUpdate"
					></quantity-select>
				</div>
				<button
					type="submit"
					class="endava h-10 w-2/4"
				>
					Purchase
				</button>
			</form>
		</section>
	</main>
	<section class="flex w-full flex-col items-center">
		<questions-section></questions-section>
	</section>
</template>
