export default defineNuxtConfig({
  extends: ['docus'],
  modules: ['@nuxt/ui'],
  css: ['~/assets/css/main.css'],
  mdc: {
    highlight: {
      shikiEngine: 'javascript',
      langs: [
        'abap', 'actionscript-3', 'ada', 'apache', 'apex', 'apl', 'applescript', 'ara', 'asm', 'astro', 'awk', 'ballerina', 'bat', 'berry', 'bibtex', 'bicep', 'blade', 'c', 'cadence', 'clarity', 'clojure', 'cmake', 'cobol', 'codeql', 'coffee', 'cpp', 'crystal', 'csharp', 'css', 'cue', 'cypher', 'd', 'dart', 'dax', 'diff', 'docker', 'dream-maker', 'elixir', 'elm', 'erb', 'erlang', 'fish', 'fsharp', 'gdresource', 'gdscript', 'gdshader', 'gherkin', 'git-commit', 'git-rebase', 'glimmer-js', 'glimmer-ts', 'glsl', 'gnuplot', 'go', 'graphql', 'groovy', 'hack', 'haml', 'handlebars', 'haskell', 'hcl', 'hlsl', 'html', 'http', 'imba', 'ini', 'java', 'javascript', 'jison', 'json', 'json5', 'jsonc', 'jsonl', 'jsx', 'julia', 'kotlin', 'kusto', 'latex', 'less', 'liquid', 'lisp', 'logo', 'lua', 'make', 'markdown', 'marko', 'matlab', 'mdc', 'mdx', 'mermaid', 'mojo', 'narrat', 'nextflow', 'nginx', 'nim', 'nix', 'objective-c', 'objective-cpp', 'ocaml', 'pascal', 'perl', 'php', 'plsql', 'postcss', 'powerquery', 'powershell', 'prisma', 'prolog', 'proto', 'pug', 'puppet', 'purescript', 'python', 'r', 'raku', 'rel', 'riscv', 'ruby', 'rust', 'sas', 'sass', 'scala', 'scheme', 'scss', 'shaderlab', 'shellscript', 'shellsession', 'solidity', 'sparql', 'sql', 'ssh-config', 'stata', 'stylus', 'svelte', 'swift', 'system-verilog', 'tasl', 'tcl', 'tex', 'toml', 'tsx', 'turtle', 'twig', 'typescript', 'v', 'vb', 'verilog', 'vhdl', 'viml', 'vue', 'vue-html', 'wasm', 'wenyan', 'wgsl', 'wolfram', 'xml', 'xsl', 'yaml', 'zenscript', 'zig'
      ]
    },
  },
  vite: {
    build: {
      sourcemap: false,
    },
    optimizeDeps: {
      include: [
        '@vue/devtools-core',
        '@vue/devtools-kit',
        '@vueuse/core',
      ]
    }
  }
})
