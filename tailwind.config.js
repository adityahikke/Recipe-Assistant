/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/main/resources/templates/**/*.html',
    './src/main/resources/static/js/**/*.js'
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#fef7ee',
          100: '#fdedd6',
          200: '#fbd7ac',
          300: '#f8bb77',
          400: '#f5953e',
          500: '#f2751a',
          600: '#e35a0f',
          700: '#bc4210',
          800: '#963514',
          900: '#792e14'
        }
      }
    }
  },
  plugins: []
};
