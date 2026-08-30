<template>
  <footer class="flex flex-col items-center">
    <a 
      href="https://nxoim.com" 
      target="_blank" 
      rel="noopener noreferrer"
      class="cursor-pointer hover:opacity-80 transition-opacity duration-200"
    >
      <img src="assets/icons/nxoim.svg" alt="Icon" class="w-48 p-8" />
    </a>
    
    <slot />
  </footer>

  <CookieBanners 
    text="No tracking or analytical cookies are used. Service and essential features provided by Cloudflare"
  />
</template>
