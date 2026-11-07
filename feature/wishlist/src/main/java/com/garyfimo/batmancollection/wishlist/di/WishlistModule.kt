package com.garyfimo.batmancollection.wishlist.di

import com.garyfimo.batmancollection.wishlist.data.FirestoreWishRepository
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import com.garyfimo.batmancollection.wishlist.presentation.addwish.AddWishViewModel
import com.garyfimo.batmancollection.wishlist.presentation.wishlist.WishlistViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wishlistModule = module {
    single<WishRepository> { FirestoreWishRepository(get()) }
    viewModelOf(::WishlistViewModel)
    viewModelOf(::AddWishViewModel)
}
