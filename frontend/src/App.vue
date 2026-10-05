<script setup>
import { computed, onMounted, ref } from 'vue'
import { ShoppingCart, X, Smartphone, CheckCircle2, AlertCircle, Store, Plus, Minus } from 'lucide-vue-next'
import { getProducts, initiateStkPush } from './services/api'

const products = ref([])
const cart = ref([])
const loading = ref(true)
const error = ref('')
const checkoutOpen = ref(false)
const paymentLoading = ref(false)
const paymentMessage = ref('')
const paymentError = ref('')
const phoneNumber = ref('')

const cartCount = computed(() =>
  cart.value.reduce((sum, item) => sum + item.quantity, 0)
)

const cartTotal = computed(() =>
  cart.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0)
)

function addToCart(product) {
  const existing = cart.value.find(item => item.id === product.id)

  if (existing) {
    existing.quantity += 1
  } else {
    cart.value.push({ ...product, quantity: 1 })
  }
}

function increase(item) {
  item.quantity += 1
}

function decrease(item) {
  if (item.quantity === 1) {
    cart.value = cart.value.filter(cartItem => cartItem.id !== item.id)
  } else {
    item.quantity -= 1
  }
}

function openCheckout() {
  paymentError.value = ''
  paymentMessage.value = ''
  checkoutOpen.value = true
}

function closeCheckout() {
  if (!paymentLoading.value) {
    checkoutOpen.value = false
  }
}

async function payWithMpesa() {
  paymentError.value = ''
  paymentMessage.value = ''

  if (!cart.value.length) {
    paymentError.value = 'Your cart is empty.'
    return
  }

  paymentLoading.value = true

  try {
    const firstProduct = cart.value.length === 1
      ? cart.value[0].name
      : `Order - ${cart.value.length} products`

    const data = await initiateStkPush({
      phoneNumber: phoneNumber.value,
      amount: cartTotal.value,
      productName: firstProduct
    })

    if (data.success) {
      paymentMessage.value =
        'STK Push sent. Check your phone and enter your M-Pesa PIN to complete payment.'
      cart.value = []
    } else {
      paymentError.value = data.responseDescription || 'M-Pesa payment could not be started.'
    }
  } catch (err) {
    paymentError.value = err.message || 'Something went wrong.'
  } finally {
    paymentLoading.value = false
  }
}

onMounted(async () => {
  try {
    products.value = await getProducts()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="app">
    <header class="navbar">
      <div class="nav-inner">
        <div class="brand">
          <div class="brand-icon"><Store :size="21" /></div>
          <div>
            <strong>Smart Store</strong>
            <span>Simple online shopping</span>
          </div>
        </div>

        <button class="cart-button" @click="openCheckout">
          <ShoppingCart :size="19" />
          <span>Cart</span>
          <b v-if="cartCount">{{ cartCount }}</b>
        </button>
      </div>
    </header>

    <main>
      <section class="hero">
        <div>
          <span class="eyebrow">WELCOME TO OUR STORE</span>
          <h1>Shop your favourite products.</h1>
          <p>Choose a product, add it to your cart and pay securely with M-Pesa.</p>
        </div>
      </section>

      <section class="products-section">
        <div class="section-heading">
          <div>
            <h2>Products</h2>
            <p>Available products</p>
          </div>
          <span>{{ products.length }} items</span>
        </div>

        <div v-if="loading" class="state">Loading products...</div>
        <div v-else-if="error" class="state error-state">{{ error }}</div>

        <div v-else class="product-grid">
          <article v-for="product in products" :key="product.id" class="product-card">
            <div class="image-wrap">
              <img :src="product.imageUrl" :alt="product.name" />
            </div>
            <div class="product-content">
              <h3>{{ product.name }}</h3>
              <p>{{ product.description }}</p>
              <div class="product-bottom">
                <strong>KES {{ Number(product.price).toLocaleString() }}</strong>
                <button @click="addToCart(product)">
                  <Plus :size="17" />
                  Add
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>

    <div v-if="cartCount" class="floating-cart" @click="openCheckout">
      <div>
        <ShoppingCart :size="20" />
        <span>{{ cartCount }} item{{ cartCount === 1 ? '' : 's' }}</span>
      </div>
      <strong>KES {{ cartTotal.toLocaleString() }}</strong>
    </div>

    <div v-if="checkoutOpen" class="modal-backdrop" @click.self="closeCheckout">
      <div class="modal">
        <button class="close-button" @click="closeCheckout">
          <X :size="20" />
        </button>

        <div class="modal-title">
          <div class="mpesa-icon"><Smartphone :size="23" /></div>
          <div>
            <h2>Checkout</h2>
            <p>Pay using M-Pesa STK Push</p>
          </div>
        </div>

        <div class="order-list">
          <div v-for="item in cart" :key="item.id" class="order-item">
            <div>
              <strong>{{ item.name }}</strong>
              <span>KES {{ Number(item.price).toLocaleString() }} × {{ item.quantity }}</span>
            </div>
            <div class="quantity">
              <button @click="decrease(item)"><Minus :size="14" /></button>
              <span>{{ item.quantity }}</span>
              <button @click="increase(item)"><Plus :size="14" /></button>
            </div>
          </div>
        </div>

        <div class="total-row">
          <span>Total</span>
          <strong>KES {{ cartTotal.toLocaleString() }}</strong>
        </div>

        <label class="field-label">M-Pesa phone number</label>
        <input
          v-model="phoneNumber"
          class="phone-input"
          type="tel"
          placeholder="0712345678"
          :disabled="paymentLoading"
        />
        <small>Example: 0712345678 or 254712345678</small>

        <div v-if="paymentMessage" class="message success">
          <CheckCircle2 :size="19" />
          <span>{{ paymentMessage }}</span>
        </div>

        <div v-if="paymentError" class="message failure">
          <AlertCircle :size="19" />
          <span>{{ paymentError }}</span>
        </div>

        <button class="pay-button" :disabled="paymentLoading || !cart.length" @click="payWithMpesa">
          <span v-if="paymentLoading" class="spinner"></span>
          <span v-else>Pay KES {{ cartTotal.toLocaleString() }} with M-Pesa</span>
          <span v-if="paymentLoading">Sending STK Push...</span>
        </button>
      </div>
    </div>
  </div>
</template>
