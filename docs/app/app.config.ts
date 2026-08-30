export default defineAppConfig({
    ui: {
        prose: {
            codeIcon: {
                kts: 'i-vscode-icons:file-type-kotlin'
            }
        },
        pageHero: {
            slots: {
                title: 'font-semibold sm:text-6xl',
                container: '!pb-0',
            },
        },
        pageCard: {
            slots: {
                container: 'lg:flex min-w-0',
                wrapper: 'flex-none',
            },
        },
        contentToc: {
            defaultVariants: {
                highlightVariant: 'circuit',
            },
        },
    },
})
