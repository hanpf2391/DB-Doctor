/**
 * DB-Doctor 演示 GIF 自动录制脚本
 *
 * 使用方法：
 * 1. 确保 DB-Doctor 已启动（http://localhost:8080）
 * 2. 安装依赖：npm install playwright playwright-core
 * 3. 运行：node scripts/record-demo-gif.js
 *
 * 依赖：
 * - ffmpeg（用于视频转 GIF）：https://ffmpeg.org/download.html
 * - playwright：npm install playwright
 */

const { chromium } = require('playwright');
const { exec } = require('child_process');
const fs = require('fs');
const path = require('path');

// 配置
const CONFIG = {
  baseUrl: 'http://localhost:8080',
  outputDir: path.join(__dirname, '../docs'),
  screenshotDir: path.join(__dirname, '../docs/screenshots-temp'),
  gifPath: path.join(__dirname, '../docs/demo.gif'),
  videoPath: path.join(__dirname, '../docs/demo-temp.mp4'),
  // 录制设置
  viewport: { width: 1280, height: 720 },
  slowMo: 500, // 慢动作模式，便于观看
  timeout: 60000,
};

// 录制步骤配置
const RECORDING_STEPS = [
  {
    name: '访问首页',
    action: async (page) => {
      await page.goto(CONFIG.baseUrl);
      await page.waitForLoadState('networkidle');
      await sleep(2000);
    },
    duration: 3000,
  },
  {
    name: '展示仪表盘',
    action: async (page) => {
      // 等待 Dashboard 加载
      await page.waitForSelector('.dashboard-container, .el-statistic-card, .el-card', { timeout: 10000 });
      await sleep(2000);
    },
    duration: 3000,
  },
  {
    name: '导航到报告列表',
    action: async (page) => {
      // 点击报告列表菜单
      const reportMenu = await page.$('text=/报告|Report|慢查询/');
      if (reportMenu) {
        await reportMenu.click();
      } else {
        // 尝试通过 URL 导航
        await page.goto(`${CONFIG.baseUrl}/#/reports`);
      }
      await page.waitForLoadState('networkidle');
      await sleep(1500);
    },
    duration: 2500,
  },
  {
    name: '查看报告详情',
    action: async (page) => {
      // 等待报告列表加载
      await page.waitForSelector('.el-table, .report-list, table', { timeout: 10000 });

      // 点击第一条记录的"详情"按钮
      const detailButton = await page.$('text=/详情|Detail|查看/');
      if (detailButton) {
        await detailButton.click();
        await sleep(2000);
      }
    },
    duration: 3000,
  },
  {
    name: '展示 AI 分析结果',
    action: async (page) => {
      // 滚动展示分析内容
      await page.evaluate(() => window.scrollBy(0, 300));
      await sleep(1500);
      await page.evaluate(() => window.scrollBy(0, 300));
      await sleep(1500);
      await page.evaluate(() => window.scrollBy(0, -600)); // 滚回顶部
      await sleep(1000);
    },
    duration: 5000,
  },
  {
    name: '导航到 AI 监控',
    action: async (page) => {
      // 点击 AI 监控菜单
      const monitorMenu = await page.$('text=/AI.*监控|Monitor|监控/');
      if (monitorMenu) {
        await monitorMenu.click();
      } else {
        await page.goto(`${CONFIG.baseUrl}/#/ai-monitor`);
      }
      await page.waitForLoadState('networkidle');
      await sleep(2000);
    },
    duration: 3000,
  },
  {
    name: '展示统计数据',
    action: async (page) => {
      // 等待图表加载
      await page.waitForSelector('.el-chart, .chart-container, .statistic', { timeout: 10000 });
      await sleep(2000);
    },
    duration: 3000,
  },
];

// 辅助函数
function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function ensureDirectories() {
  [CONFIG.outputDir, CONFIG.screenshotDir].forEach((dir) => {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  });
}

async function convertVideoToGif(videoPath, gifPath) {
  return new Promise((resolve, reject) => {
    console.log('🎬 正在转换视频为 GIF...');

    // ffmpeg 命令：控制帧率和尺寸，减小文件大小
    const cmd = `ffmpeg -i "${videoPath}" -vf "fps=15,scale=800:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse" "${gifPath}" -y`;

    exec(cmd, (error, stdout, stderr) => {
      if (error) {
        console.error('❌ 转换失败:', error.message);
        reject(error);
        return;
      }

      // 删除临时视频文件
      if (fs.existsSync(videoPath)) {
        fs.unlinkSync(videoPath);
      }

      console.log('✅ GIF 生成成功:', gifPath);
      console.log('📊 文件大小:', (fs.statSync(gifPath).size / 1024 / 1024).toFixed(2), 'MB');
      resolve();
    });
  });
}

async function recordDemo() {
  console.log('🎬 开始录制 DB-Doctor 演示...\n');

  await ensureDirectories();

  // 启动浏览器
  const browser = await chromium.launch({
    headless: false, // 显示浏览器窗口
    slowMo: CONFIG.slowMo,
  });

  const context = await browser.newContext({
    viewport: CONFIG.viewport,
    recordVideo: {
      dir: CONFIG.outputDir,
      size: CONFIG.viewport,
    },
  });

  const page = await context.newPage();

  try {
    console.log('📱 启动浏览器...');
    console.log(`🌐 访问 ${CONFIG.baseUrl}\n`);

    // 执行录制步骤
    for (let i = 0; i < RECORDING_STEPS.length; i++) {
      const step = RECORDING_STEPS[i];
      console.log(`📌 [${i + 1}/${RECORDING_STEPS.length}] ${step.name}...`);

      await step.action(page);
      await sleep(step.duration || 2000);

      console.log(`   ✅ 完成\n`);
    }

    console.log('🎥 录制完成，正在保存...');

  } catch (error) {
    console.error('❌ 录制失败:', error.message);
    throw error;
  } finally {
    await context.close();
    await browser.close();
  }

  // 获取视频文件路径
  const files = fs.readdirSync(CONFIG.outputDir);
  const videoFile = files.find(f => f.endsWith('.webm'));

  if (!videoFile) {
    throw new Error('未找到录制的视频文件');
  }

  const videoPath = path.join(CONFIG.outputDir, videoFile);
  const tempVideoPath = CONFIG.videoPath;

  // 重命名视频文件
  fs.renameSync(videoPath, tempVideoPath);

  // 转换为 GIF
  await convertVideoToGif(tempVideoPath, CONFIG.gifPath);

  console.log('\n🎉 演示 GIF 已生成！');
  console.log('📁 文件位置:', CONFIG.gifPath);
  console.log('\n📝 更新 README.md，添加以下内容：');
  console.log(`
## 🎬 功能演示

<p align="center">
  <img src="docs/demo.gif" width="800" alt="DB-Doctor 功能演示" />
</p>

**演示内容**：
1. 自动发现慢查询
2. AI Agent 智能分析
3. 生成优化建议
4. Token 成本监控
`);
}

// 运行
recordDemo().catch((error) => {
  console.error('💥 发生错误:', error);
  process.exit(1);
});
