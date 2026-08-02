const os = require('os');

const cpus = os.cpus().length;
const instances = cpus > 2 ? Math.floor(cpus / 2) : 1;

module.exports = {
  apps: [
    {
      name: 'DTunnel',
      script: './build/index.js',
      instances,
      exec_mode: 'cluster',
      env: {
        NODE_ENV: 'production',
      },
      dot_env: '.env'
    },
  ],
};
