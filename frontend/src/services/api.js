const API_BASE_URL = 'http://localhost:8081/api'

export async function getProducts() {
  const response = await fetch(`${API_BASE_URL}/products`)
  if (!response.ok) {
    throw new Error('Could not load products.')
  }
  return response.json()
}

export async function initiateStkPush(payload) {
  const response = await fetch(`${API_BASE_URL}/payments/mpesa/stk-push`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })

  const data = await response.json()

  if (!response.ok) {
    throw new Error(data.responseDescription || 'Payment request failed.')
  }

  return data
}
