/* SystemJS module definition */
/* eslint-disable no-var */
declare var module: NodeModule;
interface NodeModule {
  id: string;
}

/*
Declare TypeScript types for the plain-javascript third-party scripts.
This gets included by the compiler; its symbols do not need to be imported by
other source files but are globally available.
*/

declare var ResizeObserver: any;

/* eslint-enable no-var */

declare module 'outdated-browser-rework';
