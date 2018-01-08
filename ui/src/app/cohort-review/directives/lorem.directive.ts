import {Directive, ElementRef, OnInit, Renderer2} from '@angular/core';

@Directive({selector: '[appLorem]'})
export class LoremIpsumDirective implements OnInit {
  readonly ipsumText = `
    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
    tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
    quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
    consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
    cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
    proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  `.trim();

  constructor(private el: ElementRef, private renderer: Renderer2) {}

  ngOnInit() {
    this.renderer.appendChild(
      this.el.nativeElement,
      this.renderer.createText(this.ipsumText)
    );
  }
}
