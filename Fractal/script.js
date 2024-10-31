window.addEventListener('load', function(){
  const canvas = document.getElementById('canvas1');
  const ctx = canvas.getContext('2d');
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;

  // controls
  const randomizeButton = document.getElementById('randomizeButton');
  const slider_spread = document.getElementById('spread');
  const label_spread = document.querySelector('[for="spread"]');
  slider_spread.addEventListener('change', function(e){
      spread = e.target.value;
      updateSliders();
      drawFractal(ctx);
  });
  const slider_sides = document.getElementById('sides');
  const label_sides = document.querySelector('[for="sides"]');
  slider_sides.addEventListener('change', function(e){
      sides = e.target.value;
      updateSliders();
      drawFractal(ctx);
  });
  const slider_skew1 = document.getElementById('skew1');
  const label_skew1 = document.querySelector('[for="skew1"]');
  slider_skew1.addEventListener('change', function(e){
      skew1 = e.target.value;
      updateSliders();
      drawFractal(ctx);
  });
  const slider_skew2 = document.getElementById('skew2');
  const label_skew2 = document.querySelector('[for="skew2"]');
  slider_skew2.addEventListener('change', function(e){
      skew2 = e.target.value;
      updateSliders();
      drawFractal(ctx);
  });
  const slider_center = document.getElementById('center');
  const label_center = document.querySelector('[for="center"]');
  slider_center.addEventListener('change', function(e){
      center = e.target.value;
      updateSliders();
      drawFractal(ctx);
  });

  // canvas settings
  ctx.lineCap = 'round';
  ctx.shadowColor = 'rgba(0,0,0,0.7)';
  ctx.shadowOffsetX = 10;
  ctx.shadowOffsetY = 5;
  ctx.shadowBlur = 10;

  // effect settings
  let size = canvas.width < canvas.height ? canvas.width * 0.4 : canvas.height * 0.4;
  const maxLevel = 3;
  const scale = 0.5;
  const branches = 3;

  let spread = 1;
  let color = 'hsl(300, 100%, 50%)';
  let lineWidth = 10;
  let sides = 5;
  let skew1 = 1;
  let skew2 = 1;
  let center = 0;

  function updateSliders() {
      slider_spread.value = spread;
      label_spread.innerText = 'Spread: ' + Number(spread).toFixed(1);
      slider_sides.value = sides;
      label_sides.innerText = 'Sides: ' + sides;
      slider_skew1.value = skew1;
      label_skew1.innerText = 'Skew 1: ' + Number(skew1).toFixed(2);
      slider_skew2.value = skew2;
      label_skew2.innerText = 'Skew 2: ' + Number(skew2).toFixed(2);
      slider_center.value = center;
      label_center.innerText = 'Center: ' + center;
  }
  updateSliders();

  function drawLine(level, context){
      if (level > maxLevel) return;
      context.beginPath();
      context.moveTo(0,0);
      context.lineTo(size, 0);
      context.stroke();
      for (let i = 0; i < branches; i++){
          context.save();

          context.translate((size/branches) * i, center);
          context.scale(scale, scale);

              context.save();
              context.rotate(spread * skew1);
              drawLine(level + 1, context);
              context.restore();

              context.save();
              context.rotate(-spread * skew2);
              drawLine(level + 1, context);
              context.restore();

          context.restore();
      }
  }

  function drawFractal(context){
      context.clearRect(0, 0, canvas.width, canvas.height);
      context.strokeStyle =  color;
      context.lineWidth = lineWidth;
      context.save();
      context.translate(canvas.width/2, canvas.height/2);
      for (let i = 0; i < sides; i++){
          context.rotate((Math.PI * 2)/sides);
          drawLine(0, context);
      }
      context.restore();
  }
  drawFractal(ctx);

  function randomizeFractal(){
      spread = Math.random() * 2 + 0.5;
      color = 'hsl(' + Math.random() * 360 + ', 100%, 50%)';
      lineWidth = Math.random() * 15 + 4;
      sides = Math.floor(Math.random() * 7 + 2);
      updateSliders();
      randomizeButton.style.backgroundColor = color;
  }

  randomizeButton.addEventListener('click', function(){
      randomizeFractal();
      updateSliders();
      drawFractal(ctx);
  });

  resetButton.addEventListener('click', function(){
      skew1 = 1;
      skew2 = 1;
      center = 0;
      updateSliders();    
      drawFractal(ctx);
  });

  window.addEventListener('resize', function(){
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      ctx.shadowColor = 'rgba(0,0,0,0.7)';
      ctx.shadowOffsetX = 10;
      ctx.shadowOffsetY = 5;
      ctx.shadowBlur = 10;
      size = canvas.width < canvas.height ? canvas.width * 0.4 : canvas.height * 0.4;
      drawFractal(ctx);
  });
});