import { createRouter, createWebHistory } from 'vue-router'

import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import BookingDetailView from '../views/BookingDetailView.vue'
import MyBookingsView from '../views/MyBookingsView.vue'
import StaffDashboardView from '../views/StaffDashboardView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import RegisterView from '../views/RegisterView.vue'
import ProfileView from '../views/ProfileView.vue'
import DailyDealsView from '../views/DailyDealsView.vue'
import ForgotPasswordView from '../views/ForgotPasswordView.vue'

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        component: LoginView
    },
    { path: '/register', component: RegisterView },
    { path: '/forgot-password', component: ForgotPasswordView },
    { path: '/profile', component: ProfileView, meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
    { path: '/daily-deals', component: DailyDealsView, meta: { requiresAuth: true, roles: ['CUSTOMER'] } },
    {
        path: '/home',
        component: HomeView,
        meta: {
            requiresAuth: true
        }
    },
    {
        path: '/bookings',
        component: MyBookingsView,
        meta: {
            requiresAuth: true
        }
    },
    {
        path: '/bookings/:orderId',
        component: BookingDetailView,
        meta: {
            requiresAuth: true
        }
    },
    {
        path: '/admin',
        component: AdminDashboardView,
        meta: {
            requiresAuth: true,
            roles: ['ADMIN']
        }
    },
    {
        path: '/staff',
        component: StaffDashboardView,
        meta: {
            requiresAuth: true,
            roles: ['STAFF', 'ADMIN']
        }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to) => {

    const token =
        sessionStorage.getItem('token')

    const role =
        sessionStorage.getItem('role')


    if (to.meta.requiresAuth && !token) {
        return '/login'
    }


    if (
        to.meta.roles &&
        !to.meta.roles.includes(role)
    ) {

        if (role === 'ADMIN') {
            return '/admin'
        }

        if (role === 'STAFF') {
            return '/staff'
        }

        return '/home'
    }


    // Login remains reachable so another tab can sign in with a different account.
})

export default router
