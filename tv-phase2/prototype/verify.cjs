// Creates a browser-executable test copy. No dependencies or TV access.
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf8');
const tests = `<script>
const results=[];
function check(name,value){results.push({name,pass:!!value});if(!value)throw Error(name)}
function key(k){document.activeElement.dispatchEvent(new KeyboardEvent('keydown',{key:k,bubbles:true}))}
try{
 check('six home tiles',grid.children.length===6);
 check('initial Netflix focus',document.activeElement===grid.children[0]);
 for(const i of [0,1,2,3]){grid.children[i].click();check('home shortcut '+home[i][0],notice.textContent.startsWith(home[i][0]+' selected'))}
 key('ArrowLeft');check('left boundary stays on Netflix',document.activeElement===grid.children[0]);
 key('ArrowRight');check('right moves to YouTube',document.activeElement===grid.children[1]);
 key('ArrowDown');check('down moves to Browser',document.activeElement===grid.children[4]);
 document.activeElement.click();check('Browser reports unresolved',notice.textContent.includes('No activity'));
 key('ArrowRight');check('right moves to Media',document.activeElement===grid.children[5]);
 document.activeElement.click();check('Media opens three children',grid.children.length===3&&grid.children[0].textContent.includes('USB'));
 document.activeElement.click();check('USB reports unresolved',notice.textContent.includes('No activity'));
 for(const i of [1,2]){grid.children[i].click();check('media shortcut '+library[i][0],notice.textContent.startsWith(library[i][0]+' selected'))}
 key('ArrowDown');check('submenu reaches Back utility',document.activeElement===back);
 key('ArrowRight');check('utility right navigation',document.activeElement.id==='voice');
 key('ArrowUp');check('utility up returns to grid',document.activeElement===grid.children[2]);
 key('Escape');check('Escape restores Media focus',grid.children.length===6&&document.activeElement===grid.children[5]);
 key('ArrowDown');check('home reaches voice utility',document.activeElement.id==='voice');
 key('ArrowRight');check('Google TV utility reachable',document.activeElement.id==='google');
 document.activeElement.click();check('preview does not launch Google TV',notice.textContent.includes('without changing'));
}catch(e){results.push({error:e.message,pass:false})}
const output=document.createElement('pre');output.id='validation-results';output.textContent=JSON.stringify(results);document.body.append(output);
document.documentElement.dataset.validation=results.every(r=>r.pass)?'PASS':'FAIL';
</script>`;
fs.writeFileSync(path.join(__dirname, 'validation.html'), source.replace('</html>',tests+'</html>'));
console.log('Created prototype/validation.html. Run in headless Chrome and inspect data-validation and #validation-results.');
